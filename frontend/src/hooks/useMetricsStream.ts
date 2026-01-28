import { useState, useEffect, useRef, useCallback } from 'react';
import type { RealtimeMetrics } from '../types/agent';

const API_BASE_URL = '/api';
const UI_UPDATE_INTERVAL_MS = 2000;

interface UseMetricsStreamOptions {
  agentId?: number;
  onMetrics?: (metrics: RealtimeMetrics | RealtimeMetrics[]) => void;
  onError?: (error: Error) => void;
  onConnected?: () => void;
  onDisconnected?: () => void;
}

interface UseMetricsStreamReturn {
  metrics: Map<number, RealtimeMetrics>;
  isConnected: boolean;
  error: string | null;
  reconnect: () => void;
}

export function useMetricsStream(options: UseMetricsStreamOptions = {}): UseMetricsStreamReturn {
  const { agentId } = options;

  const [metrics, setMetrics] = useState<Map<number, RealtimeMetrics>>(new Map());
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const abortControllerRef = useRef<AbortController | null>(null);
  const reconnectTimeoutRef = useRef<number | null>(null);
  const reconnectAttemptsRef = useRef(0);
  const connectRef = useRef<() => void>(() => { });
  const isConnectingRef = useRef(false);

  const pendingMetricsRef = useRef<Map<number, RealtimeMetrics>>(new Map());
  const uiUpdateIntervalRef = useRef<number | null>(null);

  const optionsRef = useRef(options);

  useEffect(() => {
    optionsRef.current = options;
  });

  useEffect(() => {
    const connect = async () => {
      if (isConnectingRef.current) return;
      isConnectingRef.current = true;

      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
      }

      const abortController = new AbortController();
      abortControllerRef.current = abortController;

      const url = agentId
        ? `${API_BASE_URL}/metrics/stream/${agentId}`
        : `${API_BASE_URL}/metrics/stream`;

      const accessToken = localStorage.getItem('accessToken');

      try {
        const response = await fetch(url, {
          method: 'GET',
          headers: {
            'Accept': 'text/event-stream',
            'Cache-Control': 'no-cache',
            ...(accessToken && { 'Authorization': `Bearer ${accessToken}` }),
          },
          credentials: 'include',
          signal: abortController.signal,
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        if (!response.body) {
          throw new Error('ReadableStream not supported');
        }

        setIsConnected(true);
        setError(null);
        reconnectAttemptsRef.current = 0;
        optionsRef.current.onConnected?.();
        isConnectingRef.current = false;

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();

          if (done) {
            break;
          }

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';

          let eventType = 'message';
          let eventData = '';

          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventType = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              eventData = line.slice(5).trim();
            } else if (line === '' && eventData) {
              processEvent(eventType, eventData);
              eventType = 'message';
              eventData = '';
            }
          }
        }

        setIsConnected(false);
        optionsRef.current.onDisconnected?.();
        scheduleReconnect();
      } catch (err) {
        isConnectingRef.current = false;

        if (err instanceof Error && err.name === 'AbortError') {
          return;
        }

        setIsConnected(false);
        optionsRef.current.onError?.(err instanceof Error ? err : new Error(String(err)));
        optionsRef.current.onDisconnected?.();
        scheduleReconnect();
      }
    };

    const processEvent = (eventType: string, eventData: string) => {
      try {
        if (eventType === 'heartbeat' || !eventData) {
          return;
        }

        const data = JSON.parse(eventData);

        if (eventType === 'init') {
          if (Array.isArray(data)) {
            const newMetrics = new Map<number, RealtimeMetrics>();
            data.forEach((m: RealtimeMetrics) => {
              newMetrics.set(m.agentId, m);
            });
            setMetrics(newMetrics);
            data.forEach((m: RealtimeMetrics) => {
              pendingMetricsRef.current.set(m.agentId, m);
            });
            optionsRef.current.onMetrics?.(data);
          } else {
            setMetrics(new Map([[data.agentId, data]]));
            pendingMetricsRef.current.set(data.agentId, data);
            optionsRef.current.onMetrics?.(data);
          }
        } else if (eventType === 'metrics') {
          const metricsData: RealtimeMetrics = data;
          pendingMetricsRef.current.set(metricsData.agentId, metricsData);
          optionsRef.current.onMetrics?.(metricsData);
        } else if (eventType === 'metrics-all') {
          const metricsArray: RealtimeMetrics[] = data;
          metricsArray.forEach((m) => {
            pendingMetricsRef.current.set(m.agentId, m);
          });
          optionsRef.current.onMetrics?.(metricsArray);
        }
      } catch {
        // Ignore JSON parse errors for malformed messages
      }
    };

    const scheduleReconnect = () => {
      const maxAttempts = 10;
      const baseDelay = 1000;

      if (reconnectAttemptsRef.current < maxAttempts) {
        const delay = Math.min(baseDelay * Math.pow(2, reconnectAttemptsRef.current), 30000);
        reconnectAttemptsRef.current++;
        setError(`Connection lost. Reconnecting in ${delay / 1000}s...`);

        reconnectTimeoutRef.current = window.setTimeout(() => {
          connectRef.current();
        }, delay);
      } else {
        setError('Failed to connect after multiple attempts. Please refresh the page.');
      }
    };

    connectRef.current = connect;
    connect();

    uiUpdateIntervalRef.current = window.setInterval(() => {
      if (pendingMetricsRef.current.size > 0) {
        setMetrics(new Map(pendingMetricsRef.current));
      }
    }, UI_UPDATE_INTERVAL_MS);

    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
      if (uiUpdateIntervalRef.current) {
        clearInterval(uiUpdateIntervalRef.current);
        uiUpdateIntervalRef.current = null;
      }
      isConnectingRef.current = false;
    };
  }, [agentId]);

  const reconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    isConnectingRef.current = false;
    reconnectAttemptsRef.current = 0;
    connectRef.current();
  }, []);

  return { metrics, isConnected, error, reconnect };
}
