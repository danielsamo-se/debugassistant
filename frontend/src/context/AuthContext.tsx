import { createContext, useState, useEffect, ReactNode } from 'react';
import type { User, AuthResponse } from '../types';
import { login as apiLogin, register as apiRegister } from '../services/authService';

interface AuthContextType {
    user: User | null;
    isAuthenticated: boolean;
    isLoading: boolean;
    login: (email: string, password: string) => Promise<void>;
    register: (email: string, password: string, name?: string) => Promise<void>;
    logout: () => void;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    useEffect(() => {
        // load user from localStorage
        const token = localStorage.getItem('token');
        const savedUser = localStorage.getItem('user');

        if (token && savedUser) {
            try {
                setUser(JSON.parse(savedUser));
            } catch {
                // reset storage on invalid JSON
                localStorage.removeItem('token');
                localStorage.removeItem('user');
            }
        }
        setIsLoading(false);
    }, []);

    // store token and user info
    const handleAuthResponse = (response: AuthResponse) => {
        const userData: User = { email: response.email, name: response.name };
        localStorage.setItem('token', response.token);
        localStorage.setItem('user', JSON.stringify(userData));
        setUser(userData);
    };

    const login = async (email: string, password: string) => {
        const response = await apiLogin(email, password);
        handleAuthResponse(response);
    };

    const register = async (email: string, password: string, name?: string) => {
        const response = await apiRegister(email, password, name);
        handleAuthResponse(response);
    };

    // clear session data
    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
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