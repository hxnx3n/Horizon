import { createContext } from 'react';
import type { UserInfo } from '../types/auth';

export interface AuthContextType {
  user: UserInfo | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (accessToken: string, user: UserInfo) => void;
  logout: () => Promise<void>;
  checkAuth: () => Promise<boolean>;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
