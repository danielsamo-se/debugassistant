import { useEffect, useState, type ReactNode } from 'react';
import type { AuthResponse, User } from '../types';
import { AuthContext, TOKEN_KEY, USER_KEY, EXPIRES_AT_KEY } from './auth';
import {
  login as apiLogin,
  register as apiRegister,
} from '../services/authService';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    const savedUser = localStorage.getItem(USER_KEY);
    const expiresAtRaw = localStorage.getItem(EXPIRES_AT_KEY);
    const expiresAt = expiresAtRaw ? Number(expiresAtRaw) : null;

    if (token && !expiresAt) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      localStorage.removeItem(EXPIRES_AT_KEY);
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setIsLoading(false);
      return;
    }

    if (expiresAt && Date.now() > expiresAt) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      localStorage.removeItem(EXPIRES_AT_KEY);
      setIsLoading(false);
      return;
    }

    if (token && savedUser) {
      try {
        setUser(JSON.parse(savedUser));
      } catch {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
        localStorage.removeItem(EXPIRES_AT_KEY);
      }
    }
    setIsLoading(false);
  }, []);

  const handleAuthResponse = (response: AuthResponse) => {
    const userData: User = { email: response.email, name: response.name };

    localStorage.setItem(TOKEN_KEY, response.token);
    localStorage.setItem(USER_KEY, JSON.stringify(userData));
    localStorage.setItem(
      EXPIRES_AT_KEY,
      String(Date.now() + response.expiresIn),
    );

    setUser(userData);
  };

  const login = async (email: string, password: string) => {
    handleAuthResponse(await apiLogin(email, password));
  };

  const register = async (email: string, password: string, name?: string) => {
    handleAuthResponse(await apiRegister(email, password, name));
  };

  const logout = () => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(EXPIRES_AT_KEY);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}
