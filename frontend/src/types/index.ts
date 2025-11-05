export interface SearchResult {
    source: string;
    title: string;
    url: string;
    reactions: number;
    score: number;
    comments?: number;
}

export interface AnalyzeResponse {
    language: string;
    exceptionType: string;
    message: string;
    score: number;
    keywords: string[];
    rootCause: string | null;
    timestamp: string;
    results: SearchResult[];
}