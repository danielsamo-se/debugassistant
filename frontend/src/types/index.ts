export interface SearchResult {
  source: 'github' | 'stackoverflow';
  title: string;
  url: string;
  reactions: number;
  score: number;
  snippet?: string;
  answerCount?: number;
  isAnswered?: boolean;
}

export interface AnalyzeResponse {
  language: string;
  exceptionType: string;
  message: string;
  keywords: string[];
  rootCause: string | null;
  results: SearchResult[];
}

// Auth Types
export interface User {
  email: string;
  name: string | null;
}

export interface AuthResponse {
  token: string;
  expiresIn: number;
  email: string;
  name: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name?: string;
}

export interface SearchHistory {
  id: number;
  stackTraceSnippet: string;
  language: string | null;
  exceptionType: string | null;
  searchUrl: string | null;
  searchedAt: string;
}
