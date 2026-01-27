package cmd

import (
	"fmt"
	"strings"

	"github.com/hxnx3n/Horizon/agent/config"
	"github.com/hxnx3n/Horizon/agent/push"
)

func PrintUsage() {
	fmt.Printf(`Horizon Agent - System Metrics Collector (version %s)

Usage:
  horizon-agent [command] [options]

Commands:
  auth <key> <server-url>    Register this agent with the given authentication key

  run [-d]                   Start pushing metrics to the server
                             -d: Run as daemon (background mode)

  status                     Show current authentication status

  deauth                     Remove authentication configuration

  install                    Install as systemd service (Linux, requires sudo)

  uninstall                  Remove systemd service (Linux, requires sudo)

  update                     Update to the latest version

  version                    Show version information

Examples:
  horizon-agent auth hzn_abc123def456 http://myserver:8080
  horizon-agent run
  horizon-agent run -d
  sudo horizon-agent install
  horizon-agent status
  horizon-agent update`, Version)
	fmt.Println()
}

func RunAuth(args []string) error {
	if len(args) < 2 {
		return fmt.Errorf("missing arguments\nUsage: horizon-agent auth <key> <server-url>")
	}

	key := args[0]
	serverURL := strings.TrimSuffix(args[1], "/")

	fmt.Printf("Validating key with server %s...\n", serverURL)

	if err := push.ValidateKey(serverURL, key); err != nil {
		return fmt.Errorf("authentication failed: %w", err)
	}

	if err := push.StoreAuthConfig(serverURL, key); err != nil {
		return fmt.Errorf("failed to save authentication config: %w", err)
	}

	fmt.Println("✓ Authentication successful!")
	fmt.Printf("  Server: %s\n", serverURL)
	fmt.Printf("  Key: %s...%s\n", key[:8], key[len(key)-4:])
	fmt.Printf("  Config saved to: %s\n", config.GetAuthConfigPath())
	fmt.Println("\nRun 'horizon-agent run' to start pushing metrics.")

	return nil
}

func RunDeauth() error {
	authConfig, err := config.LoadAuthConfig()
	if err != nil {
		return fmt.Errorf("failed to load auth config: %w", err)
	}

	if authConfig == nil {
		fmt.Println("No authentication configured.")
		return nil
	}

	if err := config.DeleteAuthConfig(); err != nil {
		return fmt.Errorf("failed to remove auth config: %w", err)
	}

	fmt.Println("✓ Authentication removed successfully.")
	return nil
}

func RunStatus() error {
	authConfig, err := config.LoadAuthConfig()
	if err != nil {
		return fmt.Errorf("failed to load auth config: %w", err)
	}

	if authConfig == nil || !authConfig.Registered {
		fmt.Println("Status: Not authenticated")
		fmt.Println("\nRun 'horizon-agent auth <key> <server-url>' to authenticate.")
		return nil
	}

	fmt.Println("Status: Authenticated")
	fmt.Printf("  Server: %s\n", authConfig.ServerURL)
	if len(authConfig.Key) > 12 {
		fmt.Printf("  Key: %s...%s\n", authConfig.Key[:8], authConfig.Key[len(authConfig.Key)-4:])
	}
	fmt.Printf("  Config: %s\n", config.GetAuthConfigPath())

	return nil
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
