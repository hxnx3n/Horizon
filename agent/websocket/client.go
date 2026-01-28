package websocket

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"os"
	"os/exec"
	"runtime"
	"strings"
	"sync"
	"syscall"
	"unsafe"

	"github.com/creack/pty"
	"github.com/gorilla/websocket"
)

type Message struct {
	Type string `json:"type"`
	Data string `json:"data"`
	Cols int    `json:"cols,omitempty"`
	Rows int    `json:"rows,omitempty"`
}

type CommandResult struct {
	Type   string `json:"type"`
	Output string `json:"output"`
	Error  string `json:"error"`
	Code   int    `json:"code"`
}

type Client struct {
	ServerURL   string
	AgentID     string
	conn        *websocket.Conn
	connMu      sync.Mutex
	stopCh      chan struct{}
	ptyFile     *os.File
	ptyCmd      *exec.Cmd
	ptyMu       sync.Mutex
	interactive bool
}

func NewClient(serverURL, agentID string) *Client {
	return &Client{
		ServerURL:   strings.TrimSuffix(serverURL, "/"),
		AgentID:     agentID,
		stopCh:      make(chan struct{}),
		interactive: false,
	}
}

func (c *Client) Connect() error {
	wsURL := fmt.Sprintf("%s/api/ws/agent?agentId=%s", strings.Replace(c.ServerURL, "http://", "ws://", 1), c.AgentID)
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

			switch msg.Type {
			case "command":
				go c.handleCommand(msg.Data)
			case "shell_start":
				go c.startInteractiveShell(msg.Cols, msg.Rows)
			case "shell_input":
				c.handleShellInput(msg.Data)
			case "shell_resize":
				c.handleShellResize(msg.Cols, msg.Rows)
			case "shell_stop":
				c.stopInteractiveShell()
			}
		}
	}
}

func (c *Client) startInteractiveShell(cols, rows int) {
	c.ptyMu.Lock()
	defer c.ptyMu.Unlock()

	if c.ptyFile != nil {
		log.Println("Shell already running")
		return
	}

	var shell string
	if runtime.GOOS == "windows" {
		shell = "cmd.exe"
	} else {
		shell = "/bin/bash"
		if _, err := os.Stat(shell); os.IsNotExist(err) {
			shell = "/bin/sh"
		}
	}

	cmd := exec.Command(shell)
	cmd.Env = append(os.Environ(), "TERM=xterm-256color")

	ptmx, err := pty.Start(cmd)
	if err != nil {
		log.Printf("Failed to start PTY: %v", err)
		c.sendShellOutput(fmt.Sprintf("Error starting shell: %v\r\n", err))
		return
	}

	c.ptyFile = ptmx
	c.ptyCmd = cmd
	c.interactive = true

	// Set initial size
	if cols > 0 && rows > 0 {
		c.resizePtyUnsafe(cols, rows)
	}

	log.Printf("Interactive shell started (PTY)")

	// Read PTY output and send to WebSocket
	go func() {
		buf := make([]byte, 4096)
		for {
			n, err := ptmx.Read(buf)
			if err != nil {
				if err != io.EOF {
					log.Printf("PTY read error: %v", err)
				}
				c.sendShellExit()
				return
			}
			if n > 0 {
				c.sendShellOutput(string(buf[:n]))
			}
		}
	}()

	// Wait for command to finish
	go func() {
		err := cmd.Wait()
		exitCode := 0
		if err != nil {
			if exitErr, ok := err.(*exec.ExitError); ok {
				exitCode = exitErr.ExitCode()
			}
		}
		log.Printf("Shell exited with code %d", exitCode)
		c.ptyMu.Lock()
		c.ptyFile = nil
		c.ptyCmd = nil
		c.interactive = false
		c.ptyMu.Unlock()
		c.sendShellExit()
	}()
}

func (c *Client) handleShellInput(data string) {
	c.ptyMu.Lock()
	defer c.ptyMu.Unlock()

	if c.ptyFile == nil {
		return
	}

	_, err := c.ptyFile.Write([]byte(data))
	if err != nil {
		log.Printf("Failed to write to PTY: %v", err)
	}
}

func (c *Client) handleShellResize(cols, rows int) {
	c.ptyMu.Lock()
	defer c.ptyMu.Unlock()

	c.resizePtyUnsafe(cols, rows)
}

func (c *Client) resizePtyUnsafe(cols, rows int) {
	if c.ptyFile == nil {
		return
	}

	ws := struct {
		Row    uint16
		Col    uint16
		Xpixel uint16
		Ypixel uint16
	}{
		Row: uint16(rows),
		Col: uint16(cols),
	}

	syscall.SyscallN(
		syscall.SYS_IOCTL,
		c.ptyFile.Fd(),
		syscall.TIOCSWINSZ,
		uintptr(unsafe.Pointer(&ws)),
	)
}

func (c *Client) stopInteractiveShell() {
	c.ptyMu.Lock()
	defer c.ptyMu.Unlock()

	if c.ptyCmd != nil && c.ptyCmd.Process != nil {
		c.ptyCmd.Process.Signal(syscall.SIGTERM)
	}
	if c.ptyFile != nil {
		c.ptyFile.Close()
		c.ptyFile = nil
	}
	c.interactive = false
	log.Println("Interactive shell stopped")
}

func (c *Client) sendShellOutput(data string) {
	c.connMu.Lock()
	defer c.connMu.Unlock()

	msg := Message{
		Type: "shell_output",
		Data: data,
	}
	jsonData, _ := json.Marshal(msg)
	c.conn.WriteMessage(websocket.TextMessage, jsonData)
}

func (c *Client) sendShellExit() {
	c.connMu.Lock()
	defer c.connMu.Unlock()

	msg := Message{
		Type: "shell_exit",
	}
	jsonData, _ := json.Marshal(msg)
	c.conn.WriteMessage(websocket.TextMessage, jsonData)
}

func (c *Client) handleCommand(command string) {
	log.Printf("Executing command: %s", command)

	var cmd *exec.Cmd
	if runtime.GOOS == "windows" {
		cmd = exec.Command("cmd.exe", "/c", command)
	} else {
		cmd = exec.Command("/bin/sh", "-c", "sudo "+command)
	}

	output, err := cmd.CombinedOutput()
	exitCode := 0
	errMsg := ""

	if err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok {
			exitCode = exitErr.ExitCode()
		} else {
			exitCode = 1
		}
		errMsg = err.Error()
	}

	result := CommandResult{
		Type:   "command_result",
		Output: string(output),
		Error:  errMsg,
		Code:   exitCode,
	}

	c.connMu.Lock()
	defer c.connMu.Unlock()

	data, _ := json.Marshal(result)
	if err := c.conn.WriteMessage(websocket.TextMessage, data); err != nil {
		log.Printf("Failed to send command result: %v", err)
	}
}

func (c *Client) Close() error {
	c.stopInteractiveShell()
	close(c.stopCh)
	if c.conn != nil {
		return c.conn.Close()
	}
	return nil
}

func (c *Client) IsConnected() bool {
	return c.conn != nil
}
