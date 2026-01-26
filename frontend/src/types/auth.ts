export interface LoginRequest {
  email: string;
  password: string;
}

export interface UserInfo {
  id: number;
  name: string;
  email: string;
  role: string;
  profileImageUrl?: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
  user: UserInfo;
}

export interface ApiResponse<T> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}
