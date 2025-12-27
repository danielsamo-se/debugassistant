import { createContext, useEffect, useState, type ReactNode } from 'react';
import type { AuthResponse, User } from '../types';
import {
  login as apiLogin,
  register as apiRegister,
} from '../services/authService';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string, name?: string) => Promise<void>;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

const TOKEN_KEY = 'token';
const USER_KEY = 'user';
const EXPIRES_AT_KEY = 'expiresAt';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    const savedUser = localStorage.getItem(USER_KEY);
    const expiresAtRaw = localStorage.getItem(EXPIRES_AT_KEY);
    const expiresAt = expiresAtRaw ? Number(expiresAtRaw) : null;

    // if token exists but expiry missing -> reset (keeps behavior consistent)
    if (token && !expiresAt) {
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
      localStorage.removeItem(EXPIRES_AT_KEY);
      setIsLoading(false);
      return;
    }

    // if token expired, clear everything
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
