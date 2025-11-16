export interface SearchResult {
    source: "github" | "stackoverflow";
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