import { api } from './api';
import type { AnalyzeResponse } from '../types';

// call analyze endpoint
export const analyzeStackTrace = async (stackTrace: string): Promise<AnalyzeResponse> => {
    const response = await api.post<AnalyzeResponse>('/analyze', { stackTrace });
    return response.data;
};;