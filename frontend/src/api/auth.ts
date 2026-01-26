import type { LoginRequest, LoginResponse, ApiResponse } from '../types/auth';

const API_BASE_URL = '/api';

export async function login(request: LoginRequest): Promise<ApiResponse<LoginResponse>> {
  const response = await fetch(`${API_BASE_URL}/auth/login`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
    credentials: 'include',
  });

  return response.json();
}

export async function logout(): Promise<ApiResponse<void>> {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch(`${API_BASE_URL}/auth/logout`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
    },
    credentials: 'include',
  });

  localStorage.removeItem('accessToken');
  return response.json();
}

export async function refreshToken(): Promise<ApiResponse<LoginResponse>> {
  const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include',
  });

  return response.json();
}

export async function getCurrentUser(): Promise<ApiResponse<{ id: number; name: string; email: string; role: string }>> {
  const accessToken = localStorage.getItem('accessToken');

  const response = await fetch(`${API_BASE_URL}/auth/me`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken && { Authorization: `Bearer ${accessToken}` }),
    },
    credentials: 'include',
  });

  return response.json();
}
