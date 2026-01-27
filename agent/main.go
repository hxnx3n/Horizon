package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
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
		runPushMode()

	case "update":
		if err := cmd.RunUpdate(); err != nil {
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
