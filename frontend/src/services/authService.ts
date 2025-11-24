import { api } from './api';
import type { AuthResponse } from '../types';

// call login endpoint
export const login = async (
  email: string,
  password: string,
): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>('/auth/login', {
    email,
    password,
  });
  return response.data;
};

// call register endpoint
export const register = async (
  email: string,
  password: string,
  name?: string,
): Promise<AuthResponse> => {
  const response = await api.post<AuthResponse>('/auth/register', {
    email,
    password,
    name,
  });
  return response.data;
};
