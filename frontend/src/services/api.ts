import axios from 'axios';

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080/api';
const TOKEN_KEY = 'token';

export const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// attach JWT if available
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// normalize error message, keep status
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status: number | undefined = error.response?.status;
    const data = error.response?.data;

    let message: string = error.message || 'An error occurred';

    if (typeof data === 'string') {
      message = data;
    } else if (data && typeof data === 'object') {
      message = (data as any).message || JSON.stringify(data).slice(0, 300);
    }

    // if unauthorized, clear token
    if (status === 401) {
      localStorage.removeItem(TOKEN_KEY);
    }

    const err = new Error(message) as Error & { status?: number };
    err.status = status;

    return Promise.reject(err);
  },
);
