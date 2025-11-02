export interface AnalyzeResponse {
    language: string;
    exceptionType: string;
    message: string;
    score: number;
    keywords: string[];
    rootCause: string | null;
    timestamp: string;
    results: any[];
}