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

	// Get executable path
	executable, err := os.Executable()
	if err != nil {
		return fmt.Errorf("failed to get executable path: %w", err)
	}

	// Check if already authenticated
	authConfig, err := GetAuthConfig()
	if err != nil {
		return fmt.Errorf("failed to check auth config: %w", err)
	}
	if authConfig == nil {
		return fmt.Errorf("agent is not authenticated. Run 'horizon-agent auth <key> <server-url>' first")
	}

	// Create systemd service file
	serviceContent := fmt.Sprintf(systemdServiceTemplate, executable)
	if err := os.WriteFile(serviceFilePath, []byte(serviceContent), 0644); err != nil {
		return fmt.Errorf("failed to create service file: %w", err)
	}

	// Reload systemd
	if err := runCommand("systemctl", "daemon-reload"); err != nil {
		return fmt.Errorf("failed to reload systemd: %w", err)
	}

	// Enable service
	if err := runCommand("systemctl", "enable", serviceName); err != nil {
		return fmt.Errorf("failed to enable service: %w", err)
	}

	// Start service
	if err := runCommand("systemctl", "start", serviceName); err != nil {
		return fmt.Errorf("failed to start service: %w", err)
	}

	fmt.Println("✓ Horizon Agent service installed successfully!")
	fmt.Println()
	fmt.Println("Service commands:")
	fmt.Println("  sudo systemctl status horizon-agent   # Check status")
	fmt.Println("  sudo systemctl stop horizon-agent     # Stop service")
	fmt.Println("  sudo systemctl start horizon-agent    # Start service")
	fmt.Println("  sudo systemctl restart horizon-agent  # Restart service")
	fmt.Println("  sudo journalctl -u horizon-agent -f   # View logs")

	return nil
}

func RunUninstall() error {
	if runtime.GOOS != "linux" {
		return fmt.Errorf("service uninstallation is only supported on Linux")
	}

	if os.Geteuid() != 0 {
		return fmt.Errorf("this command requires root privileges. Run with sudo")
	}

	// Stop service (ignore error if not running)
	runCommand("systemctl", "stop", serviceName)

	// Disable service
	runCommand("systemctl", "disable", serviceName)

	// Remove service file
	if err := os.Remove(serviceFilePath); err != nil && !os.IsNotExist(err) {
		return fmt.Errorf("failed to remove service file: %w", err)
	}

	// Reload systemd
	if err := runCommand("systemctl", "daemon-reload"); err != nil {
		return fmt.Errorf("failed to reload systemd: %w", err)
	}

	fmt.Println("✓ Horizon Agent service uninstalled successfully!")

	return nil
}

func RunServiceStatus() error {
	if runtime.GOOS != "linux" {
		return fmt.Errorf("service status is only supported on Linux")
	}

	// Check if service file exists
	if _, err := os.Stat(serviceFilePath); os.IsNotExist(err) {
		fmt.Println("Service: Not installed")
		fmt.Println("\nRun 'sudo horizon-agent install' to install as a service.")
		return nil
	}

	// Get service status
	output, err := exec.Command("systemctl", "is-active", serviceName).Output()
	status := strings.TrimSpace(string(output))

	if err != nil || status != "active" {
		fmt.Println("Service: Installed but not running")
		fmt.Printf("Status: %s\n", status)
	} else {
		fmt.Println("Service: Running")
	}

	fmt.Println("\nUse 'sudo systemctl status horizon-agent' for detailed status.")

	return nil
}

func runCommand(name string, args ...string) error {
	cmd := exec.Command(name, args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	return cmd.Run()
}
