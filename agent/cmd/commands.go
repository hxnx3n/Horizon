package cmd

import (
	"fmt"
	"os"
	"strings"

	"github.com/hxnx3n/Horizon/agent/config"
	"github.com/hxnx3n/Horizon/agent/push"
)

func PrintUsage() {
	fmt.Printf(`Horizon Agent - System Metrics Collector (version %s)

Usage:
  horizon-agent [command] [options]

Commands:
  auth <client-key> <server-url>   Register this node with a client key
  run [-d]                         Start pushing metrics (-d: daemon mode)
  restart                          Restart the agent service
  stop                             Stop the running agent
  status                           Show agent and service status
  deauth                           Remove authentication and unregister
  version                          Show version information

Service Commands (Linux, requires sudo):
  install                          Install as systemd service
  uninstall                        Remove systemd service

Examples:
  horizon-agent auth hzn_abc123def456 http://myserver:8080
  horizon-agent run
  horizon-agent run -d
  sudo horizon-agent install
  horizon-agent status`, Version)
	fmt.Println()
}

func RunAuth(args []string) error {
	if len(args) < 2 {
		return fmt.Errorf("missing arguments\nUsage: horizon-agent auth <client-key> <server-url>")
	}

	key := args[0]
	serverURL := strings.TrimSuffix(args[1], "/")

	existingConfig, err := config.LoadAuthConfig()
	if err != nil {
		return fmt.Errorf("failed to check existing config: %w", err)
	}

	nodeID := getNodeID()

	if existingConfig != nil && existingConfig.Registered {
		if existingConfig.Key == key && existingConfig.ServerURL == serverURL {
			fmt.Println("✓ Already authenticated with the same configuration.")
			fmt.Printf("  Server: %s\n", serverURL)
			fmt.Printf("  Node: %s\n", existingConfig.NodeID)
			return nil
		}

		fmt.Println("Warning: Agent is already authenticated.")
		fmt.Printf("  Current Server: %s\n", existingConfig.ServerURL)
		fmt.Printf("  Current Node: %s\n", existingConfig.NodeID)
		fmt.Println()
		fmt.Print("Do you want to re-authenticate with new settings? (y/N): ")

		var response string
		fmt.Scanln(&response)
		if strings.ToLower(response) != "y" && strings.ToLower(response) != "yes" {
			fmt.Println("Authentication cancelled.")
			return nil
		}
	}

	fmt.Printf("Validating client key with server %s...\n", serverURL)

	if err := push.ValidateKey(serverURL, key); err != nil {
		return fmt.Errorf("authentication failed: %w", err)
	}

	authConfig := &config.AuthConfig{
		Key:        key,
		ServerURL:  serverURL,
		NodeID:     nodeID,
		Registered: true,
	}

	if err := config.SaveAuthConfig(authConfig); err != nil {
		return fmt.Errorf("failed to save authentication config: %w", err)
	}

	fmt.Println("✓ Authentication successful!")
	fmt.Printf("  Server: %s\n", serverURL)
	fmt.Printf("  Node ID: %s\n", nodeID)
	fmt.Printf("  Key: %s...%s\n", key[:8], key[len(key)-4:])
	fmt.Printf("  Config: %s\n", config.GetAuthConfigPath())
	fmt.Println("\nRun 'horizon-agent run' to start pushing metrics.")

	return nil
}

func getNodeID() string {
	nodeID := os.Getenv("NODE_ID")
	if nodeID == "" {
		hostname, err := os.Hostname()
		if err != nil {
			nodeID = "unknown"
		} else {
			nodeID = hostname
		}
	}
	return nodeID
}

func RunDeauth() error {
	authConfig, err := config.LoadAuthConfig()
	if err != nil {
		return fmt.Errorf("failed to load auth config: %w", err)
	}

	if authConfig == nil || !authConfig.Registered {
		fmt.Println("No authentication configured.")
		return nil
	}

	fmt.Printf("Removing authentication for node: %s\n", authConfig.NodeID)

	if err := config.DeleteAuthConfig(); err != nil {
		return fmt.Errorf("failed to remove auth config: %w", err)
	}

	fmt.Println("✓ Authentication removed successfully.")
	fmt.Println("  The agent will no longer push metrics to the server.")
	return nil
}

func RunStatus() error {
	authConfig, err := config.LoadAuthConfig()
	if err != nil {
		return fmt.Errorf("failed to load auth config: %w", err)
	}

	fmt.Println("=== Horizon Agent Status ===")
	fmt.Println()

	if authConfig == nil || !authConfig.Registered {
		fmt.Println("Authentication: Not configured")
		fmt.Println("\nRun 'horizon-agent auth <client-key> <server-url>' to authenticate.")
	} else {
		fmt.Println("Authentication: Configured")
		fmt.Printf("  Server: %s\n", authConfig.ServerURL)
		fmt.Printf("  Node ID: %s\n", authConfig.NodeID)
		if len(authConfig.Key) > 12 {
			fmt.Printf("  Key: %s...%s\n", authConfig.Key[:8], authConfig.Key[len(authConfig.Key)-4:])
		}
		fmt.Printf("  Config: %s\n", config.GetAuthConfigPath())
	}

	RunServiceStatus()

	return nil
}

func RunStop() error {
	return stopRunningAgent()
}

func GetAuthConfig() (*config.AuthConfig, error) {
	authConfig, err := config.LoadAuthConfig()
	if err != nil {
		return nil, err
	}

	if authConfig == nil || !authConfig.Registered {
		return nil, nil
	}

	return authConfig, nil
}
