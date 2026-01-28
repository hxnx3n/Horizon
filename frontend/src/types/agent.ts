export interface Agent {
  id: number;
  userId?: number;
  clientKeyId?: number;
  name: string;
  nodeId?: string;
  hostname?: string;
  os?: string;
  platform?: string;
  enabled: boolean;
  lastSeenAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface DiskInfo {
  device: string;
  mountpoint: string;
  totalBytes: number;
  usedBytes: number;
  usage: number;
}

export interface NetworkInterface {
  name: string;
  ips: string[];
  sentBytes: number;
  recvBytes: number;
  sentRate: number;
  recvRate: number;
}

export interface RealtimeMetrics {
  agentId: number;
  agentName: string;
  hostname: string | null;
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
  networkRxRate: number | null;
  networkTxRate: number | null;
  loadAverage1m: number | null;
  loadAverage5m: number | null;
  loadAverage15m: number | null;
  processCount: number | null;
  uptimeSeconds: number | null;
  temperature: number | null;
  disks: DiskInfo[] | null;
  interfaces: NetworkInterface[] | null;
  nodeId: string | null;
  os: string | null;
  platform: string | null;
  timestamp: string | null;
  lastHeartbeat: string | null;
}

export interface MetricsHistoryPoint {
  timestamp: number;
  cpuUsage: number | null;
  memoryUsage: number | null;
  diskUsage: number | null;
  networkRxRate: number | null;
  networkTxRate: number | null;
  temperature: number | null;
  interfaceStats?: Record<string, { rx: number; tx: number }>;
}

export interface AgentMetricsHistory {
  agentId: number;
  history: MetricsHistoryPoint[];
  maxPoints: number;
}
