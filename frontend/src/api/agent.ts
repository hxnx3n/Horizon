import type { Agent, AgentCreateRequest, AgentUpdateRequest } from '../types/agent';
import type { ApiResponse } from '../types/auth';

const API_BASE_URL = '/api';

function getAuthHeaders(): HeadersInit {
  const accessToken = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
  };
}

export async function getAgents(enabled?: boolean): Promise<ApiResponse<Agent[]>> {
  const url = enabled !== undefined
    ? `${API_BASE_URL}/agents?enabled=${enabled}`
    : `${API_BASE_URL}/agents`;

  const response = await fetch(url, {
    method: 'GET',
    headers: getAuthHeaders(),
    credentials: 'include',
  });

  return response.json();
}

export async function getAgent(id: number): Promise<ApiResponse<Agent>> {
  const response = await fetch(`${API_BASE_URL}/agents/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
    credentials: 'include',
  });

  return response.json();
}

export async function createAgent(request: AgentCreateRequest): Promise<ApiResponse<Agent>> {
  const response = await fetch(`${API_BASE_URL}/agents`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
    credentials: 'include',
  });

  return response.json();
}

export async function updateAgent(id: number, request: AgentUpdateRequest): Promise<ApiResponse<Agent>> {
  const response = await fetch(`${API_BASE_URL}/agents/${id}`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
    credentials: 'include',
  });

  return response.json();
}

export async function deleteAgent(id: number): Promise<ApiResponse<void>> {
  const response = await fetch(`${API_BASE_URL}/agents/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
    credentials: 'include',
  });

  return response.json();
}
