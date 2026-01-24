package metrics

import (
	"encoding/json"
	"time"
)

type DiskMetrics struct {
	Device     string  `json:"device"`
	Mountpoint string  `json:"mountpoint"`
	TotalBytes uint64  `json:"totalBytes"`
	UsedBytes  uint64  `json:"usedBytes"`
	Usage      float64 `json:"usage"`
}

type InterfaceMetrics struct {
	Name      string   `json:"name"`
	IPs       []string `json:"ips"`
	SentBytes uint64   `json:"sentBytes"`
	RecvBytes uint64   `json:"recvBytes"`
	SentRate  float64  `json:"sentRate"`
	RecvRate  float64  `json:"recvRate"`
}

type Metrics struct {
	NodeID   string  `json:"nodeId"`
	OS       string  `json:"os"`
	Platform string  `json:"platform"`
	CPU      float64 `json:"cpu"`
	Temp     float64 `json:"temp"`

	MemUsage float64 `json:"memUsage"`
	MemTotal uint64  `json:"memTotalBytes"`
	MemUsed  uint64  `json:"memUsedBytes"`

	Disks []DiskMetrics `json:"disks"`

	Interfaces []InterfaceMetrics `json:"interfaces"`

	Status    string    `json:"status"`
	Timestamp time.Time `json:"-"`
}

func (m *Metrics) MarshalJSON() ([]byte, error) {
	type Alias Metrics
	return json.Marshal(&struct {
		*Alias
		Timestamp string `json:"timestamp"`
	}{
		Alias:     (*Alias)(m),
		Timestamp: m.Timestamp.UTC().Truncate(time.Millisecond).Format(time.RFC3339),
	})
}
