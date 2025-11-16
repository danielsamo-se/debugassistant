import { useState } from 'react';
import './App.css';
import type { AnalyzeResponse } from "./types";
import { analyzeStackTrace } from "./services/analyzeService";
import StackTraceInput from "./components/StackTraceInput";
import ResultDisplay from "./components/ResultDisplay";

export default function App() {
    const [result, setResult] = useState<AnalyzeResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleAnalyze = async (trace: string) => {
        setLoading(true);
        setError(null);
        setResult(null);

        try {
            const data = await analyzeStackTrace(trace);
            setResult(data);
        } catch (err) {
            setError(err instanceof Error ? err.message : 'Unknown error occurred');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container">
            <h1>🔍 Debug Assistant</h1>
            <p className="subtitle">
                Paste your stack trace and find solutions from Stack Overflow and GitHub
            </p>

            <StackTraceInput
                onAnalyze={handleAnalyze}
                loading={loading}
            />

            {error && <div className="error-box">{error}</div>}

            {result && <ResultDisplay result={result} />}
        </div>
    );
}