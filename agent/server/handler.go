package server

import (
	"encoding/json"
	"net/http"

	"github.com/hxnx3n/Horizon/agent/metrics"
)

type Handler struct {
	collector *metrics.Collector
}

func NewHandler(c *metrics.Collector) *Handler {
	return &Handler{collector: c}
}

func (h *Handler) RegisterRoutes(mux *http.ServeMux) {
	mux.HandleFunc("/metrics", h.handleMetrics)
}

func (h *Handler) handleMetrics(w http.ResponseWriter, r *http.Request) {
	data := h.collector.GetMetrics()
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(data)
}
