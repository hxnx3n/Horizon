package config

import (
	"os"
	"strconv"
	"time"

	"github.com/joho/godotenv"
)

type Config struct {
	Port          int
	NodeID        string
	CacheInterval time.Duration
}

func Load() *Config {
	_ = godotenv.Load()

	port := 9090
	if portStr := os.Getenv("AGENT_PORT"); portStr != "" {
		if p, err := strconv.Atoi(portStr); err == nil && p > 0 {
			port = p
		}
	}

	nodeID := os.Getenv("NODE_ID")
	if nodeID == "" {
		hostname, err := os.Hostname()
		if err != nil {
			nodeID = "unknown"
		} else {
			nodeID = hostname
		}
	}

	cacheInterval := 1 * time.Second
	if intervalStr := os.Getenv("CACHE_INTERVAL"); intervalStr != "" {
		if d, err := time.ParseDuration(intervalStr); err == nil {
			cacheInterval = d
		}
	}

	return &Config{
		Port:          port,
		NodeID:        nodeID,
		CacheInterval: cacheInterval,
	}
}
