export interface Agent {
  id: number;
  name: string;
  ip: string;
  port: number;
  pollingInterval: number;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AgentCreateRequest {
  name: string;
  ip: string;
  port?: number;
  pollingInterval?: number;
}

export interface AgentUpdateRequest {
  name?: string;
  ip?: string;
  port?: number;
  pollingInterval?: number;
  enabled?: boolean;
}

export interface RealtimeMetrics {
  agentId: number;
  agentName: string;
  agentIp: string;
  online: boolean;
  cpuUsage: number | null;
  memoryTotal: number | null;
  memoryUsed: number | null;
  memoryUsage: number | null;
  diskTotal: number | null;
  diskUsed: number | null;
  diskUsage: number | null;
  networkRxBytes: number | null;
  networkTxBytes: number | null;
  loadAverage1m: number | null;
  loadAverage5m: number | null;
  loadAverage15m: number | null;
  processCount: number | null;
  uptimeSeconds: number | null;
  timestamp: string | null;
  lastHeartbeat: string | null;
}
