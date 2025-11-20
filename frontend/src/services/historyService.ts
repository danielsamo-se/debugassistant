import { api } from './api';
import type { SearchHistory } from '../types';

export interface SaveHistoryRequest {
    stackTraceSnippet: string;
    language?: string;
    exceptionType?: string;
    searchUrl?: string;
}

// save one history entry
export const saveHistory = async (data: SaveHistoryRequest): Promise<SearchHistory> => {
    const response = await api.post<SearchHistory>('/history', data);
    return response.data;
};

// load user history
export const getHistory = async (): Promise<SearchHistory[]> => {
    const response = await api.get<SearchHistory[]>('/history');
    return response.data;
};