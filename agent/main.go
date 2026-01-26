package main

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/hxnx3n/Horizon/agent/config"
	"github.com/hxnx3n/Horizon/agent/metrics"
	"github.com/hxnx3n/Horizon/agent/server"
)

func main() {
	cfg := config.Load()

	collector := metrics.NewCollector(cfg.NodeID, cfg.CacheInterval)
	handler := server.NewHandler(collector)
	mux := http.NewServeMux()
	handler.RegisterRoutes(mux)

	srv := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.Port),
		Handler: mux,
	}

	var pusher *metrics.Pusher
	if cfg.PushEnabled && cfg.BackendURL != "" {
		pusher = metrics.NewPusher(collector, cfg.BackendURL, cfg.PushInterval)
		pusher.Start()
	}

	go func() {
		log.Printf("Agent started on port %d", cfg.Port)
		if cfg.PushEnabled && cfg.BackendURL != "" {
			log.Printf("Push mode enabled: %s (interval: %v)", cfg.BackendURL, cfg.PushInterval)
		}
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Listen error: %v", err)
		}
	}()

	quit := make(chan os.Signal, 1)
	signal.Notify(quit, os.Interrupt, syscall.SIGTERM)
	<-quit

	if pusher != nil {
		pusher.Stop()
	}

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	srv.Shutdown(ctx)
	log.Println("Agent stopped")
}
