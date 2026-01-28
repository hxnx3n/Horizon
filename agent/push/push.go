package push

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/hxnx3n/Horizon/agent/metrics"
)

type PushClient struct {
	serverURL  string
	key        string
	httpClient *http.Client
	collector  *metrics.Collector
	interval   time.Duration
	port       int
	stopCh     chan struct{}
	AgentID    string
}

type RegisterRequest struct {
	Key      string `json:"key"`
	NodeID   string `json:"nodeId"`
	Hostname string `json:"hostname"`
	OS       string `json:"os"`
	Platform string `json:"platform"`
	Port     int    `json:"port"`
}

type RegisterResponse struct {
	StatusCode int `json:"statusCode"`
	Result     *struct {
		ID        string `json:"id"`
		NodeID    string `json:"nodeId"`
		Hostname  string `json:"hostname"`
		OS        string `json:"os"`
		Platform  string `json:"platform"`
		IPAddress string `json:"ipAddress"`
	} `json:"result"`
	ErrorCode    string `json:"errorCode"`
	ErrorMessage string `json:"errorMessage"`
}

type PushRequest struct {
	Key     string          `json:"key"`
	Metrics *MetricsPayload `json:"metrics"`
}

type MetricsPayload struct {
	NodeID       string                     `json:"nodeId"`
	OS           string                     `json:"os"`
	Platform     string                     `json:"platform"`
	CPU          float64                    `json:"cpuUsage"`
	Temp         float64                    `json:"temperature"`
	MemUsage     float64                    `json:"memUsage"`
	MemTotal     uint64                     `json:"memoryTotal"`
	MemUsed      uint64                     `json:"memoryUsed"`
	Disks        []metrics.DiskMetrics      `json:"disks"`
	Interfaces   []metrics.InterfaceMetrics `json:"interfaces"`
	Uptime       uint64                     `json:"uptimeSeconds"`
	ProcessCount uint64                     `json:"processCount"`
	Status       string                     `json:"status"`
	Timestamp    string                     `json:"timestamp"`
}

func NewPushClient(serverURL, key string, collector *metrics.Collector, interval time.Duration, port int) *PushClient {
	return &PushClient{
		serverURL: serverURL,
		key:       key,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
		collector: collector,
		interval:  interval,
		port:      port,
		stopCh:    make(chan struct{}),
	}
}

func (c *PushClient) Register() error {
	m := c.collector.GetMetrics()

	reqBody := RegisterRequest{
		Key:      c.key,
		NodeID:   m.NodeID,
		Hostname: m.NodeID,
		OS:       m.OS,
		Platform: m.Platform,
		Port:     c.port,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return fmt.Errorf("failed to marshal register request: %w", err)
	}

	req, err := http.NewRequest("POST", c.serverURL+"/api/agent/register", bytes.NewBuffer(jsonData))
	if err != nil {
		return fmt.Errorf("failed to create register request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Agent-Key", c.key)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send register request: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("failed to read register response: %w", err)
	}

	if resp.StatusCode != http.StatusCreated && resp.StatusCode != http.StatusOK {
		var errResp RegisterResponse
		if err := json.Unmarshal(body, &errResp); err == nil && errResp.ErrorMessage != "" {
			return fmt.Errorf("registration failed: %s", errResp.ErrorMessage)
		}
		return fmt.Errorf("registration failed with status %d: %s", resp.StatusCode, string(body))
	}

	var response RegisterResponse
	if err := json.Unmarshal(body, &response); err == nil && response.Result != nil {
		c.AgentID = response.Result.ID
	}

	return nil
}

func (c *PushClient) PushMetrics() error {
	m := c.collector.GetMetrics()

	payload := &MetricsPayload{
		NodeID:       m.NodeID,
		OS:           m.OS,
		Platform:     m.Platform,
		CPU:          m.CPU,
		Temp:         m.Temp,
		MemUsage:     m.MemUsage,
		MemTotal:     m.MemTotal,
		MemUsed:      m.MemUsed,
		Disks:        m.Disks,
		Interfaces:   m.Interfaces,
		Uptime:       m.Uptime,
		ProcessCount: m.ProcessCount,
		Status:       m.Status,
		Timestamp:    m.Timestamp.UTC().Format(time.RFC3339),
	}

	reqBody := PushRequest{
		Key:     c.key,
		Metrics: payload,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return fmt.Errorf("failed to marshal metrics: %w", err)
	}

	req, err := http.NewRequest("POST", c.serverURL+"/api/agent/push", bytes.NewBuffer(jsonData))
	if err != nil {
		return fmt.Errorf("failed to create push request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Agent-Key", c.key)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to send metrics: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("push failed with status %d: %s", resp.StatusCode, string(body))
	}

	return nil
}

func (c *PushClient) StartPushing() {
	ticker := time.NewTicker(c.interval)
	defer ticker.Stop()

	if err := c.PushMetrics(); err != nil {
		fmt.Printf("Failed to push metrics: %v\n", err)
	}

	for {
		select {
		case <-ticker.C:
			if err := c.PushMetrics(); err != nil {
				fmt.Printf("Failed to push metrics: %v\n", err)
			}
		case <-c.stopCh:
			return
		}
	}
}

func (c *PushClient) Stop() {
	close(c.stopCh)
}

func ValidateKey(serverURL, key string) error {
	client := &http.Client{Timeout: 10 * time.Second}

	jsonData, err := json.Marshal(key)
	if err != nil {
		return fmt.Errorf("failed to marshal request: %w", err)
	}

	req, err := http.NewRequest("POST", serverURL+"/api/agent/validate", bytes.NewBuffer(jsonData))
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Agent-Key", key)

	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to connect to server: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusUnauthorized || resp.StatusCode == http.StatusBadRequest {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("invalid or expired key: %s", string(body))
	}

	if resp.StatusCode != http.StatusCreated && resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("validation failed with status %d: %s", resp.StatusCode, string(body))
	}

	return nil
}
