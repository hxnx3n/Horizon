package push

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"strings"
	"time"

	"github.com/hxnx3n/Horizon/agent/metrics"
)

// containsAuthError checks if the response body contains authentication-related error messages
func containsAuthError(body string) bool {
	lowerBody := strings.ToLower(body)
	return strings.Contains(lowerBody, "invalid") && strings.Contains(lowerBody, "authentication") ||
		strings.Contains(lowerBody, "expired") && strings.Contains(lowerBody, "key") ||
		strings.Contains(lowerBody, "invalid or expired")
}

type PushClient struct {
	serverURL    string
	key          string
	httpClient   *http.Client
	collector    *metrics.Collector
	interval     time.Duration
	port         int
	stopCh       chan struct{}
	AgentID      string
	authInvalid  bool
	authErrorMsg string
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
	Success bool   `json:"success"`
	Message string `json:"message"`
	Data    *struct {
		ID        int64  `json:"id"`
		NodeID    string `json:"nodeId"`
		Hostname  string `json:"hostname"`
		OS        string `json:"os"`
		Platform  string `json:"platform"`
		CreatedAt string `json:"createdAt"`
	} `json:"data"`
	Timestamp string `json:"timestamp"`
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
		collector:    collector,
		interval:     interval,
		port:         port,
		stopCh:       make(chan struct{}),
		authInvalid:  false,
		authErrorMsg: "",
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

	// 인증 오류 감지 (401 Unauthorized, 403 Forbidden)
	if resp.StatusCode == http.StatusUnauthorized || resp.StatusCode == http.StatusForbidden {
		c.authInvalid = true
		c.authErrorMsg = fmt.Sprintf("key invalid or expired (HTTP %d): %s", resp.StatusCode, string(body))
		return fmt.Errorf("authentication failed: %s", c.authErrorMsg)
	}

	if resp.StatusCode != http.StatusCreated && resp.StatusCode != http.StatusOK {
		var errResp RegisterResponse
		if err := json.Unmarshal(body, &errResp); err == nil && errResp.Message != "" {
			return fmt.Errorf("registration failed: %s", errResp.Message)
		}
		return fmt.Errorf("registration failed with status %d: %s", resp.StatusCode, string(body))
	}

	var response RegisterResponse
	if err := json.Unmarshal(body, &response); err == nil && response.Data != nil {
		c.AgentID = fmt.Sprintf("%d", response.Data.ID)
	} else if err != nil {
		log.Printf("Failed to unmarshal register response: %v, body: %s", err, string(body))
	}

	return nil
}

func (c *PushClient) PushMetrics() error {
	// 인증이 무효화된 경우 푸시 중단
	if c.authInvalid {
		return fmt.Errorf("authentication invalid: %s", c.authErrorMsg)
	}

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

	body, _ := io.ReadAll(resp.Body)

	// 인증 오류 감지 (400 Bad Request with auth message, 401 Unauthorized, 403 Forbidden)
	if resp.StatusCode == http.StatusUnauthorized || resp.StatusCode == http.StatusForbidden ||
		(resp.StatusCode == http.StatusBadRequest && containsAuthError(string(body))) {
		c.authInvalid = true
		c.authErrorMsg = fmt.Sprintf("key invalid or expired (HTTP %d): %s", resp.StatusCode, string(body))
		return fmt.Errorf("authentication failed: %s", c.authErrorMsg)
	}

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("push failed with status %d: %s", resp.StatusCode, string(body))
	}

	return nil
}

func (c *PushClient) StartPushing() {
	ticker := time.NewTicker(c.interval)
	defer ticker.Stop()

	if err := c.PushMetrics(); err != nil {
		fmt.Printf("Failed to push metrics: %v\n", err)
		if c.authInvalid {
			c.handleAuthFailure()
			return
		}
	}

	for {
		select {
		case <-ticker.C:
			// 인증이 무효화된 경우 푸시 중단
			if c.authInvalid {
				c.handleAuthFailure()
				return
			}
			if err := c.PushMetrics(); err != nil {
				fmt.Printf("Failed to push metrics: %v\n", err)
				if c.authInvalid {
					c.handleAuthFailure()
					return
				}
			}
		case <-c.stopCh:
			return
		}
	}
}

func (c *PushClient) handleAuthFailure() {
	log.Println()
	log.Println("╔════════════════════════════════════════════════════════════╗")
	log.Println("║  AUTHENTICATION FAILED - METRICS PUSH STOPPED              ║")
	log.Println("╠════════════════════════════════════════════════════════════╣")
	log.Printf("║  Error: %s\n", c.authErrorMsg)
	log.Println("║                                                            ║")
	log.Println("║  To resume metrics push:                                   ║")
	log.Println("║  1. Get a new valid client key from the dashboard          ║")
	log.Println("║  2. Run: horizon-agent auth <new-key> <server-url>         ║")
	log.Println("║  3. Restart the agent: horizon-agent run                   ║")
	log.Println("╚════════════════════════════════════════════════════════════╝")
	log.Println()
}

func (c *PushClient) IsAuthValid() bool {
	return !c.authInvalid
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
