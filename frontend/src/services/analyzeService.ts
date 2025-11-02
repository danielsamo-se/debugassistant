import type { AnalyzeResponse } from "../types";

const API_URL = "http://localhost:8080/api/analyze";

export const analyzeStackTrace = async (stackTrace: string): Promise<AnalyzeResponse> => {
    const response = await fetch(API_URL, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ stackTrace }),
    });

    if (!response.ok) {
        throw new Error(`Error: ${response.status} ${response.statusText}`);
    }

    return response.json();
};