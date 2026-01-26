export interface Agent {
  id: number;
  name: string;
  ip: string;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AgentCreateRequest {
  name: string;
  ip: string;
}

export interface AgentUpdateRequest {
  name?: string;
  ip?: string;
  enabled?: boolean;
}
