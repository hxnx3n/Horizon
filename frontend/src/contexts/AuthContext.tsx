import { useState, useEffect, type ReactNode } from 'react';
import { getCurrentUser, logout as apiLogout, refreshToken } from '../api/auth';
import type { UserInfo } from '../types/auth';
import { AuthContext } from './authContextDef';

export type { AuthContextType } from './authContextDef';
export { AuthContext } from './authContextDef';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  const isAuthenticated = !!user;

  const login = (accessToken: string, userInfo: UserInfo) => {
    localStorage.setItem('accessToken', accessToken);
    setUser(userInfo);
  };

  const logout = async () => {
    await apiLogout();
    localStorage.removeItem('accessToken');
    setUser(null);

  };

  const checkAuth = async (): Promise<boolean> => {
    const accessToken = localStorage.getItem('accessToken');

    if (!accessToken) {
      const refreshResponse = await refreshToken();
      if (refreshResponse.success && refreshResponse.data) {
        localStorage.setItem('accessToken', refreshResponse.data.accessToken);
        setUser(refreshResponse.data.user);
        return true;
      }
      setUser(null);
      return false;
    }

    try {
      const response = await getCurrentUser();
      if (response.success && response.data) {
        setUser(response.data as UserInfo);
        return true;
      } else {
        const refreshResponse = await refreshToken();
        if (refreshResponse.success && refreshResponse.data) {
          localStorage.setItem('accessToken', refreshResponse.data.accessToken);
          setUser(refreshResponse.data.user);
          return true;
        }
        localStorage.removeItem('accessToken');
        setUser(null);
        return false;
      }
    } catch {
      localStorage.removeItem('accessToken');
      setUser(null);
      return false;
    }
  };

  useEffect(() => {
    const init = async () => {
      await checkAuth();
      setIsLoading(false);
    };
    init();
  }, []);

  return (
    <AuthContext.Provider value={{ user, isAuthenticated, isLoading, login, logout, checkAuth }}>
      {children}
    </AuthContext.Provider>
  );
}
