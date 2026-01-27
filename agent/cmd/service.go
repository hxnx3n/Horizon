package cmd

import (
	"fmt"
	"os"
	"os/exec"
	"runtime"
	"strings"
)

const systemdServiceTemplate = `[Unit]
Description=Horizon Agent - System Metrics Collector
After=network.target

[Service]
Type=simple
ExecStart=%s run
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
`

const serviceName = "horizon-agent"
const serviceFilePath = "/etc/systemd/system/horizon-agent.service"

func RunInstall() error {
	if runtime.GOOS != "linux" {
		return fmt.Errorf("service installation is only supported on Linux")
	}

	if os.Geteuid() != 0 {
		return fmt.Errorf("this command requires root privileges. Run with sudo")
	}

	executable, err := os.Executable()
	if err != nil {
		return fmt.Errorf("failed to get executable path: %w", err)
	}

	authConfig, err := GetAuthConfig()
	if err != nil {
		return fmt.Errorf("failed to check auth config: %w", err)
	}
	if authConfig == nil {
		return fmt.Errorf("agent is not authenticated. Run 'horizon-agent auth <client-key> <server-url>' first")
	}

	serviceContent := fmt.Sprintf(systemdServiceTemplate, executable)
	if err := os.WriteFile(serviceFilePath, []byte(serviceContent), 0644); err != nil {
		return fmt.Errorf("failed to create service file: %w", err)
	}

	if err := runCommand("systemctl", "daemon-reload"); err != nil {
		return fmt.Errorf("failed to reload systemd: %w", err)
	}

	if err := runCommand("systemctl", "enable", serviceName); err != nil {
		return fmt.Errorf("failed to enable service: %w", err)
	}

	if err := runCommand("systemctl", "start", serviceName); err != nil {
		return fmt.Errorf("failed to start service: %w", err)
	}

	fmt.Println("✓ Horizon Agent service installed successfully!")
	fmt.Println()
	fmt.Println("Service commands:")
	fmt.Println("  horizon-agent status    # Check status")
	fmt.Println("  horizon-agent restart   # Restart service")
	fmt.Println("  horizon-agent stop      # Stop service")

	return nil
}

func RunUninstall() error {
	if runtime.GOOS != "linux" {
		return fmt.Errorf("service uninstallation is only supported on Linux")
	}

	if os.Geteuid() != 0 {
		return fmt.Errorf("this command requires root privileges. Run with sudo")
	}

	if _, err := os.Stat(serviceFilePath); os.IsNotExist(err) {
		fmt.Println("Service is not installed.")
		return nil
	}

	fmt.Println("This will:")
	fmt.Println("  - Stop the Horizon Agent service")
	fmt.Println("  - Remove the systemd service configuration")
	fmt.Println()
	fmt.Print("Are you sure you want to uninstall? (y/N): ")

	var response string
	fmt.Scanln(&response)
	if strings.ToLower(response) != "y" && strings.ToLower(response) != "yes" {
		fmt.Println("Uninstall cancelled.")
		return nil
	}

	runCommand("systemctl", "stop", serviceName)
	runCommand("systemctl", "disable", serviceName)

	if err := os.Remove(serviceFilePath); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("failed to remove service file: %w", err)
	}

	if err := runCommand("systemctl", "daemon-reload"); err != nil {
		return fmt.Errorf("failed to reload systemd: %w", err)
	}

	fmt.Println()
	fmt.Println("✓ Horizon Agent service uninstalled successfully!")
	fmt.Println()
	fmt.Println("Note: Authentication config is preserved.")
	fmt.Println("  To remove authentication, run: horizon-agent deauth")

	return nil
}

func RunRestart() error {
	if runtime.GOOS == "linux" {
		if _, err := os.Stat(serviceFilePath); err == nil {
			if os.Geteuid() != 0 {
				return fmt.Errorf("restarting service requires root privileges. Run with sudo")
			}
			if err := runCommand("systemctl", "restart", serviceName); err != nil {
				return fmt.Errorf("failed to restart service: %w", err)
			}
			fmt.Println("✓ Service restarted successfully!")
			return nil
		}
	}

	if err := stopRunningAgent(); err != nil {
		fmt.Printf("Note: %v\n", err)
	}
	fmt.Println("Run 'horizon-agent run' or 'horizon-agent run -d' to start again.")
	return nil
}

func RunServiceStatus() error {
	if runtime.GOOS != "linux" {
		return nil
	}

	fmt.Println()
	if _, err := os.Stat(serviceFilePath); os.IsNotExist(err) {
		fmt.Println("Service: Not installed")
		return nil
	}

	output, err := exec.Command("systemctl", "is-active", serviceName).Output()
	status := strings.TrimSpace(string(output))

	if err != nil || status != "active" {
		fmt.Printf("Service: Installed (status: %s)\n", status)
	} else {
		fmt.Println("Service: Running")
	}

	return nil
}

func stopRunningAgent() error {
	if runtime.GOOS == "linux" {
		if _, err := os.Stat(serviceFilePath); err == nil {
			output, _ := exec.Command("systemctl", "is-active", serviceName).Output()
			if strings.TrimSpace(string(output)) == "active" {
				if os.Geteuid() != 0 {
					return fmt.Errorf("stopping service requires root privileges. Run with sudo")
				}
				if err := runCommand("systemctl", "stop", serviceName); err != nil {
					return fmt.Errorf("failed to stop service: %w", err)
				}
				fmt.Println("✓ Service stopped successfully!")
				return nil
			}
		}
	}

	if runtime.GOOS == "windows" {
		exec.Command("taskkill", "/F", "/IM", "horizon-agent.exe").Run()
	} else {
		exec.Command("pkill", "-f", "horizon-agent").Run()
	}

	fmt.Println("✓ Stop signal sent to running agents.")
	return nil
}

func runCommand(name string, args ...string) error {
	cmd := exec.Command(name, args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}
