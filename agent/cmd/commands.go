package cmd

import (
	"fmt"
	"os"
	"strings"

	"github.com/hxnx3n/Horizon/agent/config"
	"github.com/hxnx3n/Horizon/agent/push"
)

const defaultServerURL = "http://localhost:8080"

func PrintUsage() {
	fmt.Println(`Horizon Agent - System Metrics Collector

Usage:
  horizon-agent [command] [options]

Commands:
  auth <key> [server-url]    Register this agent with the given authentication key
                             Optional: specify server URL (default: http://localhost:8080)

  run                        Start pushing metrics to the server

  status                     Show current authentication status

  deauth                     Remove authentication configuration

Examples:
  horizon-agent auth hzn_abc123def456 http://myserver:8080
  horizon-agent run
  horizon-agent status
  horizon-agent deauth

Environment Variables:
  HORIZON_SERVER_URL   Backend server URL (required)
  HORIZON_KEY          Agent authentication key (required, or use 'auth' command)
  NODE_ID              Custom node identifier (default: hostname)
  AGENT_PORT           Local health check port (default: 9090)
  CACHE_INTERVAL       Metrics collection interval (default: 1s)`)
}

func RunAuth(args []string) error {
	if len(args) < 1 {
		return fmt.Errorf("missing authentication key\nUsage: horizon-agent auth <key> [server-url]")
	}

	key := args[0]
	serverURL := defaultServerURL

	if len(args) > 1 {
		serverURL = args[1]
	}

	if envURL := os.Getenv("HORIZON_SERVER_URL"); envURL != "" {
		serverURL = envURL
	}

	serverURL = strings.TrimSuffix(serverURL, "/")

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
		fmt.Println("\nRun 'horizon-agent auth <key>' to authenticate.")
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
	envKey := os.Getenv("HORIZON_KEY")
	envServer := os.Getenv("HORIZON_SERVER_URL")

	if envKey != "" {
		serverURL := defaultServerURL
		if envServer != "" {
			serverURL = envServer
		}
		return &config.AuthConfig{
			Key:        envKey,
			ServerURL:  serverURL,
			Registered: true,
		}, nil
	}

	authConfig, err := config.LoadAuthConfig()
	if err != nil {
		return nil, err
	}

	if authConfig == nil || !authConfig.Registered {
		return nil, nil
	}

	if envServer != "" {
		authConfig.ServerURL = envServer
	}

	return authConfig, nil
}
