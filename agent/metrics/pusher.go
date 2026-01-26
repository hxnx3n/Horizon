package metrics

import (
	"bytes"
	"context"
	"encoding/json"
	"log"
	"net/http"
	"sync"
	"time"
)

type Pusher struct {
	collector    *Collector
	backendURL   string
	pushInterval time.Duration
	httpClient   *http.Client
	ctx          context.Context
	cancel       context.CancelFunc
	wg           sync.WaitGroup
}

func NewPusher(collector *Collector, backendURL string, pushInterval time.Duration) *Pusher {
	ctx, cancel := context.WithCancel(context.Background())
	return &Pusher{
		collector:    collector,
		backendURL:   backendURL,
		pushInterval: pushInterval,
		httpClient: &http.Client{
			Timeout: 5 * time.Second,
		},
		ctx:    ctx,
		cancel: cancel,
	}
}

func (p *Pusher) Start() {
	p.wg.Add(1)
	go p.run()
	log.Printf("Pusher started, sending to %s every %v", p.backendURL, p.pushInterval)
}

func (p *Pusher) Stop() {
	p.cancel()
	p.wg.Wait()
	log.Println("Pusher stopped")
}

func (p *Pusher) run() {
	defer p.wg.Done()

	ticker := time.NewTicker(p.pushInterval)
	defer ticker.Stop()

	p.push()

	for {
		select {
		case <-p.ctx.Done():
			return
		case <-ticker.C:
			p.push()
		}
	}
}

func (p *Pusher) push() {
	metrics := p.collector.GetMetrics()

	data, err := json.Marshal(metrics)
	if err != nil {
		log.Printf("Failed to marshal metrics: %v", err)
		return
	}

	req, err := http.NewRequestWithContext(p.ctx, "POST", p.backendURL+"/api/metrics/report", bytes.NewReader(data))
	if err != nil {
		log.Printf("Failed to create request: %v", err)
		return
	}

	req.Header.Set("Content-Type", "application/json")

	resp, err := p.httpClient.Do(req)
	if err != nil {
		log.Printf("Failed to push metrics: %v", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		log.Printf("Backend returned error status: %d", resp.StatusCode)
	}
}
