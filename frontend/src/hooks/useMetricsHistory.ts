import { useState, useCallback, useRef, useEffect } from 'react';
import type { RealtimeMetrics, MetricsHistoryPoint, AgentMetricsHistory } from '../types/agent';

const DEFAULT_MAX_POINTS = 60;
const UPDATE_INTERVAL_MS = 1500;

interface UseMetricsHistoryOptions {
  maxPoints?: number;
}

interface UseMetricsHistoryReturn {
  histories: Map<number, AgentMetricsHistory>;
  addMetrics: (metrics: RealtimeMetrics) => void;
  addMultipleMetrics: (metricsArray: RealtimeMetrics[]) => void;
  getHistory: (agentId: number) => MetricsHistoryPoint[];
  clearHistory: (agentId: number) => void;
  clearAllHistories: () => void;
}

export function useMetricsHistory(options: UseMetricsHistoryOptions = {}): UseMetricsHistoryReturn {
  const { maxPoints = DEFAULT_MAX_POINTS } = options;
  const [histories, setHistories] = useState<Map<number, AgentMetricsHistory>>(new Map());
  
  const latestMetricsRef = useRef<Map<number, MetricsHistoryPoint>>(new Map());
  const intervalRef = useRef<number | null>(null);

  useEffect(() => {
    intervalRef.current = window.setInterval(() => {
      const latest = latestMetricsRef.current;
      if (latest.size === 0) return;

      setHistories((prev) => {
        const newHistories = new Map(prev);

        latest.forEach((point, agentId) => {
          const existing = newHistories.get(agentId);
          const newPoint = { ...point, timestamp: Date.now() };

          if (existing) {
            const newHistory = [...existing.history, newPoint];
            if (newHistory.length > maxPoints) {
              newHistory.shift();
            }
            newHistories.set(agentId, {
              ...existing,
              history: newHistory,
            });
          } else {
            newHistories.set(agentId, {
              agentId,
              history: [newPoint],
              maxPoints,
            });
          }
        });

        return newHistories;
      });
    }, UPDATE_INTERVAL_MS);

    return () => {
      if (intervalRef.current !== null) {
        clearInterval(intervalRef.current);
      }
    };
  }, [maxPoints]);

  const addMetrics = useCallback((metrics: RealtimeMetrics) => {
    if (!metrics.online) return;

    const prevPoint = latestMetricsRef.current.get(metrics.agentId);

    const point: MetricsHistoryPoint = {
      timestamp: Date.now(),
      cpuUsage: metrics.cpuUsage,
      memoryUsage: metrics.memoryUsage,
      diskUsage: metrics.diskUsage,
      networkRxRate: metrics.networkRxRate ?? prevPoint?.networkRxRate ?? null,
      networkTxRate: metrics.networkTxRate ?? prevPoint?.networkTxRate ?? null,
      temperature: metrics.temperature,
    };

    latestMetricsRef.current.set(metrics.agentId, point);
  }, []);

  const addMultipleMetrics = useCallback((metricsArray: RealtimeMetrics[]) => {
    metricsArray.forEach((metrics) => {
      addMetrics(metrics);
    });
  }, [addMetrics]);

  const getHistory = useCallback((agentId: number): MetricsHistoryPoint[] => {
    return histories.get(agentId)?.history ?? [];
  }, [histories]);

  const clearHistory = useCallback((agentId: number) => {
    setHistories((prev) => {
      const newHistories = new Map(prev);
      newHistories.delete(agentId);
      return newHistories;
    });
    latestMetricsRef.current.delete(agentId);
  }, []);

  const clearAllHistories = useCallback(() => {
    setHistories(new Map());
    latestMetricsRef.current.clear();
  }, []);

  return {
    histories,
    addMetrics,
    addMultipleMetrics,
    getHistory,
    clearHistory,
    clearAllHistories,
  };
}
