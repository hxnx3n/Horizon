package websocket

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/exec"
	"strings"

	"github.com/gorilla/websocket"
)

type Message struct {
	Type string `json:"type"`
	Data string `json:"data"`
}

type CommandResult struct {
	Type   string `json:"type"`
	Output string `json:"output"`
	Error  string `json:"error"`
	Code   int    `json:"code"`
}

type Client struct {
	ServerURL string
	AgentID   string
	conn      *websocket.Conn
	stopCh    chan struct{}
}

func NewClient(serverURL, agentID string) *Client {
	return &Client{
		ServerURL: strings.TrimSuffix(serverURL, "/"),
		AgentID:   agentID,
		stopCh:    make(chan struct{}),
	}
}

func (c *Client) Connect() error {
	wsURL := fmt.Sprintf("%s/ws/agent?agentId=%s", strings.Replace(c.ServerURL, "http://", "ws://", 1), c.AgentID)
	log.Printf("Connecting to WebSocket: %s", wsURL)

	conn, _, err := websocket.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		return fmt.Errorf("failed to connect to WebSocket: %w", err)
	}

	c.conn = conn
	log.Printf("Connected to server at %s", c.ServerURL)

	go c.listen()
	return nil
}

func (c *Client) listen() {
	for {
		select {
		case <-c.stopCh:
			return
		default:
			var msg Message
			err := c.conn.ReadJSON(&msg)
			if err != nil {
				if websocket.IsUnexpectedCloseError(err, websocket.CloseGoingAway, websocket.CloseAbnormalClosure) {
					log.Printf("WebSocket error: %v", err)
				}
				return
			}

			if msg.Type == "command" {
				go c.handleCommand(msg.Data)
			}
		}
	}
}

func (c *Client) handleCommand(command string) {
	log.Printf("Executing command: %s", command)

	var cmd *exec.Cmd
	if os.Getenv("HORIZON_OS") == "windows" || len(os.Args) > 0 {
		cmd = exec.Command("cmd.exe", "/c", command)
	} else {
		cmd = exec.Command("/bin/sh", "-c", command)
	}

	output, err := cmd.CombinedOutput()
	exitCode := 0
	errMsg := ""

	if err != nil {
		exitCode = 1
		errMsg = err.Error()
	}

	result := CommandResult{
		Type:   "command_result",
		Output: string(output),
		Error:  errMsg,
		Code:   exitCode,
	}

	data, _ := json.Marshal(result)
	c.conn.WriteMessage(websocket.TextMessage, data)
}

func (c *Client) Close() error {
	close(c.stopCh)
	if c.conn != nil {
		return c.conn.Close()
	}
	return nil
}

func (c *Client) IsConnected() bool {
	return c.conn != nil
}
