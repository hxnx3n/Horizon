import type { ApiResponse } from '../types/auth';

const API_BASE_URL = '/api';

function getAuthHeaders(): HeadersInit {
  const accessToken = localStorage.getItem('accessToken');
  return {
    'Content-Type': 'application/json',
    ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
  };
}

export interface ClientKey {
  id: number;
  name: string;
  description?: string;
  enabled: boolean;
  expired?: boolean;
  expiresAt?: string;
  lastUsedAt?: string;
  createdAt: string;
  maskedKey: string;
}

export interface ClientKeyCreateRequest {
  name: string;
  description?: string;
  expiresInDays?: number;
}

export interface ClientKeyCreatedResponse {
  id: number;
  name: string;
  description?: string;
  keyValue: string;
  enabled: boolean;
  expiresAt?: string;
  createdAt: string;
}

export async function getClientKeys(): Promise<ApiResponse<ClientKey[]>> {
  const response = await fetch(`${API_BASE_URL}/client-keys`, {
    method: 'GET',
    headers: getAuthHeaders(),
    credentials: 'include',
  });

  return response.json();
}

export async function getClientKey(id: number): Promise<ApiResponse<ClientKey>> {
  const response = await fetch(`${API_BASE_URL}/client-keys/${id}`, {
    method: 'GET',
    headers: getAuthHeaders(),
    credentials: 'include',
  });

  return response.json();
}

export async function createClientKey(request: ClientKeyCreateRequest): Promise<ApiResponse<ClientKeyCreatedResponse>> {
  const response = await fetch(`${API_BASE_URL}/client-keys`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(request),
    credentials: 'include',
  });

  return response.json();
}

export async function revokeClientKey(id: number): Promise<ApiResponse<void>> {
  const response = await fetch(`${API_BASE_URL}/client-keys/${id}/revoke`, {
    method: 'POST',
    headers: getAuthHeaders(),
    credentials: 'include',
  });

  return response.json();
}

export async function deleteClientKey(id: number): Promise<ApiResponse<void>> {
  const response = await fetch(`${API_BASE_URL}/client-keys/${id}`, {
    method: 'DELETE',
    headers: getAuthHeaders(),
    credentials: 'include',
  });

  return response.json();
}
