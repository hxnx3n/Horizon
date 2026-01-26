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

type Collector struct {
	nodeID        string
	cache         *Metrics
	cacheMutex    sync.RWMutex
	cacheInterval time.Duration
	lastUpdate    time.Time

	prevNetIO   map[string]net.IOCountersStat
	prevNetTime time.Time

	lastCPU  float64
	lastTemp float64
}

func NewCollector(nodeID string, interval time.Duration) *Collector {
	return &Collector{
		nodeID:        nodeID,
		cacheInterval: interval,
		prevNetIO:     make(map[string]net.IOCountersStat),
		lastTemp:      45.0,
	}
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

	hInfo, _ := host.Info()
	vMem, _ := mem.VirtualMemory()

	m := &Metrics{
		NodeID:     c.nodeID,
		OS:         hInfo.OS,
		Platform:   hInfo.Platform,
		CPU:        c.collectCPU(),
		Temp:       c.collectTemp(),
		MemUsage:   round(vMem.UsedPercent, 1),
		MemTotal:   vMem.Total,
		MemUsed:    vMem.Used,
		Disks:      make([]DiskMetrics, 0),
		Interfaces: make([]InterfaceMetrics, 0),
		Status:     "RUNNING",
		Timestamp:  now.UTC(),
	}

	partitions, _ := disk.Partitions(false)
	for _, p := range partitions {
		usage, err := disk.Usage(p.Mountpoint)
		if err != nil || usage.Total == 0 {
			continue
		}

		m.Disks = append(m.Disks, DiskMetrics{
			Device:     p.Device,
			Mountpoint: p.Mountpoint,
			TotalBytes: usage.Total,
			UsedBytes:  usage.Used,
			Usage:      round(usage.UsedPercent, 1),
		})
	}

	ipMap := make(map[string][]string)
	ifaces, _ := net.Interfaces()
	for _, iface := range ifaces {
		var ips []string
		for _, addr := range iface.Addrs {
			ips = append(ips, addr.Addr)
		}
		ipMap[iface.Name] = ips
	}

	netIOs, _ := net.IOCounters(true)
	var duration float64
	if !c.prevNetTime.IsZero() {
		duration = now.Sub(c.prevNetTime).Seconds()
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

		if prev, ok := c.prevNetIO[io.Name]; ok && duration > 0 {
			iface.SentRate = round(float64(io.BytesSent-prev.BytesSent)/1024/duration, 2)
			iface.RecvRate = round(float64(io.BytesRecv-prev.BytesRecv)/1024/duration, 2)
		}

		m.Interfaces = append(m.Interfaces, iface)
		c.prevNetIO[io.Name] = io
	}

	c.prevNetTime = now
	c.cache = m
	c.lastUpdate = now

	ret := *m
	return &ret
}

func (c *Collector) collectCPU() float64 {
	percentages, err := cpu.Percent(500*time.Millisecond, false)
	if err == nil && len(percentages) > 0 && percentages[0] > 0 {
		c.lastCPU = round(percentages[0], 1)
	} else if c.lastCPU == 0 {
		perCPU, err := cpu.Percent(500*time.Millisecond, true)
		if err == nil && len(perCPU) > 0 {
			var total float64
			for _, p := range perCPU {
				total += p
			}
			c.lastCPU = round(total/float64(len(perCPU)), 1)
		}
	}
	return c.lastCPU
}

func (c *Collector) collectTemp() float64 {
	temps, _ := host.SensorsTemperatures()
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
