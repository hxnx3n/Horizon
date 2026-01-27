package metrics

import (
	"sync"
	"time"

	"github.com/shirou/gopsutil/v3/cpu"
	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v3/host"
	"github.com/shirou/gopsutil/v3/mem"
	"github.com/shirou/gopsutil/v3/net"
)

type netRateData struct {
	sentRate float64
	recvRate float64
}

type Collector struct {
	nodeID        string
	cache         *Metrics
	cacheMutex    sync.RWMutex
	cacheInterval time.Duration
	lastUpdate    time.Time

	prevNetIO   map[string]net.IOCountersStat
	netRates    map[string]netRateData
	prevNetTime time.Time
	netMutex    sync.Mutex

	prevCPUTimes []cpu.TimesStat
	prevCPUTime  time.Time
	cpuMutex     sync.Mutex

	lastCPU  float64
	lastTemp float64

	hostInfo     *host.InfoStat
	hostInfoOnce sync.Once
}

func NewCollector(nodeID string, interval time.Duration) *Collector {
	c := &Collector{
		nodeID:        nodeID,
		cacheInterval: interval,
		prevNetIO:     make(map[string]net.IOCountersStat),
		netRates:      make(map[string]netRateData),
		lastTemp:      0,
	}

	c.prevCPUTimes, _ = cpu.Times(false)
	c.prevCPUTime = time.Now()

	go c.startBackgroundCollection()

	return c
}

func (c *Collector) startBackgroundCollection() {
	ticker := time.NewTicker(500 * time.Millisecond)
	defer ticker.Stop()

	for range ticker.C {
		c.collectCPUAsync()
		c.collectNetworkAsync()
	}
}

func (c *Collector) collectCPUAsync() {
	c.cpuMutex.Lock()
	defer c.cpuMutex.Unlock()

	currentTimes, err := cpu.Times(false)
	if err != nil || len(currentTimes) == 0 {
		return
	}

	if len(c.prevCPUTimes) > 0 {
		prev := c.prevCPUTimes[0]
		curr := currentTimes[0]

		prevTotal := prev.User + prev.System + prev.Idle + prev.Nice + prev.Iowait + prev.Irq + prev.Softirq + prev.Steal
		currTotal := curr.User + curr.System + curr.Idle + curr.Nice + curr.Iowait + curr.Irq + curr.Softirq + curr.Steal

		prevIdle := prev.Idle + prev.Iowait
		currIdle := curr.Idle + curr.Iowait

		totalDelta := currTotal - prevTotal
		idleDelta := currIdle - prevIdle

		if totalDelta > 0 {
			c.lastCPU = round((1.0-idleDelta/totalDelta)*100.0, 1)
		}
	}

	c.prevCPUTimes = currentTimes
	c.prevCPUTime = time.Now()
}

func (c *Collector) collectNetworkAsync() {
	c.netMutex.Lock()
	defer c.netMutex.Unlock()

	netIOs, err := net.IOCounters(true)
	if err != nil {
		return
	}

	now := time.Now()
	if c.prevNetTime.IsZero() {
		for _, io := range netIOs {
			c.prevNetIO[io.Name] = io
		}
		c.prevNetTime = now
		return
	}

	duration := now.Sub(c.prevNetTime).Seconds()
	if duration > 0 {
		for _, io := range netIOs {
			if prev, ok := c.prevNetIO[io.Name]; ok {
				sentDiff := io.BytesSent - prev.BytesSent
				recvDiff := io.BytesRecv - prev.BytesRecv
				c.netRates[io.Name] = netRateData{
					sentRate: round(float64(sentDiff)/duration, 2),
					recvRate: round(float64(recvDiff)/duration, 2),
				}
			}
			c.prevNetIO[io.Name] = io
		}
	}
	c.prevNetTime = now
}

func (c *Collector) GetMetrics() *Metrics {
	c.cacheMutex.RLock()
	if time.Since(c.lastUpdate) < c.cacheInterval && c.cache != nil {
		m := *c.cache
		m.Timestamp = time.Now().UTC()
		c.cacheMutex.RUnlock()
		return &m
	}
	c.cacheMutex.RUnlock()

	return c.updateCache()
}

func (c *Collector) updateCache() *Metrics {
	c.cacheMutex.Lock()
	defer c.cacheMutex.Unlock()

	now := time.Now()

	c.hostInfoOnce.Do(func() {
		c.hostInfo, _ = host.Info()
	})

	var osName, platform string
	var uptime, processCount uint64
	if c.hostInfo != nil {
		osName = c.hostInfo.OS
		platform = c.hostInfo.Platform
	}

	if info, err := host.Info(); err == nil {
		uptime = info.Uptime
		processCount = info.Procs
	}

	var memUsage float64
	var memTotal, memUsed uint64
	if vMem, err := mem.VirtualMemory(); err == nil {
		memTotal = vMem.Total
		memUsed = vMem.Total - vMem.Available
		if memTotal > 0 {
			memUsage = round(float64(memUsed)/float64(memTotal)*100.0, 1)
		}
	}

	m := &Metrics{
		NodeID:       c.nodeID,
		OS:           osName,
		Platform:     platform,
		CPU:          c.getCPU(),
		Temp:         c.collectTemp(),
		MemUsage:     memUsage,
		MemTotal:     memTotal,
		MemUsed:      memUsed,
		Disks:        c.collectDisks(),
		Interfaces:   c.collectInterfaces(now),
		Uptime:       uptime,
		ProcessCount: processCount,
		Status:       "RUNNING",
		Timestamp:    now.UTC(),
	}

	c.cache = m
	c.lastUpdate = now

	ret := *m
	return &ret
}

func (c *Collector) getCPU() float64 {
	c.cpuMutex.Lock()
	defer c.cpuMutex.Unlock()
	return c.lastCPU
}

func (c *Collector) collectDisks() []DiskMetrics {
	disks := make([]DiskMetrics, 0)

	partitions, err := disk.Partitions(false)
	if err != nil {
		return disks
	}

	seen := make(map[string]bool)
	for _, p := range partitions {
		if seen[p.Device] {
			continue
		}

		usage, err := disk.Usage(p.Mountpoint)
		if err != nil || usage.Total == 0 {
			continue
		}

		seen[p.Device] = true
		disks = append(disks, DiskMetrics{
			Device:     p.Device,
			Mountpoint: p.Mountpoint,
			TotalBytes: usage.Total,
			UsedBytes:  usage.Used,
			Usage:      round(usage.UsedPercent, 1),
		})
	}

	return disks
}

func (c *Collector) collectInterfaces(now time.Time) []InterfaceMetrics {
	interfaces := make([]InterfaceMetrics, 0)

	ipMap := make(map[string][]string)
	ifaces, _ := net.Interfaces()
	for _, iface := range ifaces {
		var ips []string
		for _, addr := range iface.Addrs {
			ips = append(ips, addr.Addr)
		}
		ipMap[iface.Name] = ips
	}

	c.netMutex.Lock()
	defer c.netMutex.Unlock()

	netIOs, err := net.IOCounters(true)
	if err != nil {
		return interfaces
	}

	for _, io := range netIOs {
		if io.Name == "" {
			continue
		}

		iface := InterfaceMetrics{
			Name:      io.Name,
			IPs:       ipMap[io.Name],
			SentBytes: io.BytesSent,
			RecvBytes: io.BytesRecv,
		}

		if rate, ok := c.netRates[io.Name]; ok {
			iface.SentRate = rate.sentRate
			iface.RecvRate = rate.recvRate
		}

		interfaces = append(interfaces, iface)
	}

	return interfaces
}

func (c *Collector) collectTemp() float64 {
	temps, err := host.SensorsTemperatures()
	if err != nil {
		return c.lastTemp
	}

	var total float64
	var count int
	for _, t := range temps {
		if t.Temperature > 0 && t.Temperature < 150 {
			total += t.Temperature
			count++
		}
	}

	if count > 0 {
		c.lastTemp = round(total/float64(count), 1)
	}

	return c.lastTemp
}

func round(val float64, precision int) float64 {
	ratio := 1.0
	for i := 0; i < precision; i++ {
		ratio *= 10
	}
	return float64(int(val*ratio+0.5)) / ratio
}
