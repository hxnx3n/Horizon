package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/exec"
	"os/signal"
	"syscall"
	"time"

	"github.com/hxnx3n/Horizon/agent/cmd"
	"github.com/hxnx3n/Horizon/agent/config"
	"github.com/hxnx3n/Horizon/agent/metrics"
	"github.com/hxnx3n/Horizon/agent/push"
)

func main() {
	if len(os.Args) < 2 {
		cmd.PrintUsage()
		os.Exit(1)
	}

	command := os.Args[1]

	switch command {
	case "auth":
		if err := cmd.RunAuth(os.Args[2:]); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

	case "deauth":
		if err := cmd.RunDeauth(); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

	case "status":
		if err := cmd.RunStatus(); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

	case "run":
		daemonMode := len(os.Args) > 2 && (os.Args[2] == "-d" || os.Args[2] == "--daemon")
		if daemonMode {
			runAsDaemon()
		} else {
			runPushMode()
		}

	case "restart":
		if err := cmd.RunRestart(); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

	case "stop":
		if err := cmd.RunStop(); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

	case "install":
		if err := cmd.RunInstall(); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

	case "uninstall":
		if err := cmd.RunUninstall(); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

	case "update":
		var targetVersion string
		if len(os.Args) > 2 {
			targetVersion = os.Args[2]
		}
		if err := cmd.RunUpdate(targetVersion); err != nil {
			fmt.Fprintf(os.Stderr, "Error: %v\n", err)
			os.Exit(1)
		}

	case "version", "-v", "--version":
		cmd.RunVersion()

	case "help", "-h", "--help":
		cmd.PrintUsage()

	default:
		fmt.Fprintf(os.Stderr, "Unknown command: %s\n\n", command)
		cmd.PrintUsage()
		os.Exit(1)
	}
}

func runPushMode() {
	authConfig, err := cmd.GetAuthConfig()
	if err != nil {
		log.Fatalf("Failed to load auth config: %v", err)
	}

	if authConfig == nil {
		fmt.Println("Agent is not authenticated.")
		fmt.Println()
		fmt.Println("To authenticate, run:")
		fmt.Println("  horizon-agent auth <your-api-key> <server-url>")
		fmt.Println()
		fmt.Println("Example:")
		fmt.Println("  horizon-agent auth hzn_abc123 http://localhost:8080")
		os.Exit(1)
	}

	cfg := config.Load()
	collector := metrics.NewCollector(cfg.NodeID, cfg.CacheInterval)

	pushClient := push.NewPushClient(
		authConfig.ServerURL,
		authConfig.Key,
		collector,
		cfg.CacheInterval,
		cfg.Port,
	)

	log.Printf("Registering with server %s...", authConfig.ServerURL)
	if err := pushClient.Register(); err != nil {
		log.Fatalf("Failed to register with server: %v", err)
	}
	log.Printf("Registered successfully!")

	go func() {
		log.Printf("Starting metrics push (interval: %s)", cfg.CacheInterval)
		pushClient.StartPushing()
	}()

	mux := http.NewServeMux()
	mux.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusOK)
		w.Write([]byte("OK"))
	})
	mux.HandleFunc("/metrics", func(w http.ResponseWriter, r *http.Request) {
		data := collector.GetMetrics()
		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(data)
	})
	mux.HandleFunc("/command", func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost {
			w.WriteHeader(http.StatusMethodNotAllowed)
			return
		}

		var req struct {
			Command string `json:"command"`
		}
		if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
			w.WriteHeader(http.StatusBadRequest)
			json.NewEncoder(w).Encode(map[string]interface{}{
				"output":   "",
				"error":    err.Error(),
				"exitCode": 1,
			})
			return
		}

		output, err := executeCommand(req.Command)
		exitCode := 0
		errMsg := ""
		if err != nil {
			exitCode = 1
			errMsg = err.Error()
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]interface{}{
			"output":   output,
			"error":    errMsg,
			"exitCode": exitCode,
		})
	})

	srv := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.Port),
		Handler: mux,
	}

	go func() {
		log.Printf("Health check server started on port %d", cfg.Port)
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Printf("Health server error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt, syscall.SIGTERM)
	<-quit

	log.Println("Shutting down...")
	pushClient.Stop()

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	srv.Shutdown(ctx)

	log.Println("Agent stopped")
}

func runAsDaemon() {
	if os.Getenv("HORIZON_DAEMON") == "1" {
		runPushMode()
		return
	}

	executable, err := os.Executable()
	if err != nil {
		log.Fatalf("Failed to get executable path: %v", err)
	}

	cmd := exec.Command(executable, "run")
	cmd.Env = append(os.Environ(), "HORIZON_DAEMON=1")
	cmd.Stdout = nil
	cmd.Stderr = nil
	cmd.Stdin = nil

	if err := cmd.Start(); err != nil {
		log.Fatalf("Failed to start daemon: %v", err)
	}

	fmt.Printf("✓ Agent started in background (PID: %d)\n", cmd.Process.Pid)
	fmt.Println("  Use 'horizon-agent status' to check status")
	fmt.Println("  Use 'pkill horizon-agent' to stop")
}

func executeCommand(command string) (string, error) {
	var cmd *exec.Cmd

	if os.Getenv("HORIZON_OS") == "windows" || len(os.Args) > 0 {
		// Windows
		cmd = exec.Command("cmd.exe", "/c", command)
	} else {
		// Unix-like (Linux, macOS)
		cmd = exec.Command("/bin/sh", "-c", command)
	}

	output, err := cmd.CombinedOutput()
	return string(output), err
}
