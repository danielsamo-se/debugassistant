import type { AnalyzeResponse } from "../types";
import ResultCard from "./ResultCard";

interface Props {
    result: AnalyzeResponse;
}

export default function ResultDisplay({ result }: Props) {
    const githubCount = result.results.filter(r => r.source === "github").length;
    const soCount = result.results.filter(r => r.source === "stackoverflow").length;

    return (
        <div className="result-section">
            <div className="summary">
                <div className="badge">
                    Language: <strong>{result.language.toUpperCase()}</strong>
                </div>
                <div className="badge">
                    Type: <strong>{result.exceptionType}</strong>
                </div>
                {result.rootCause && (
                    <div className="badge">
                        Root Cause: <strong>{result.rootCause}</strong>
                    </div>
                )}
            </div>

            {result.keywords.length > 0 && (
                <div className="keywords">
                    {result.keywords.map((kw, i) => (
                        <span key={i} className="keyword-tag">{kw}</span>
                    ))}
                </div>
            )}

            <h3 className="results-header">
                Found {result.results.length} Solutions
                <span className="source-counts">
                    ({soCount} Stack Overflow, {githubCount} GitHub)
                </span>
            </h3>

            <div className="results-list">
                {result.results.length === 0 ? (
                    <p className="no-results">
                        No matches found. Try a different stack trace.
                    </p>
                ) : (
                    result.results.map((item, index) => (
                        <ResultCard
                            key={index}
                            result={item}
                            isTopMatch={index === 0 && item.score > 0.5}
                        />
                    ))
                )}
            </div>
        </div>
    );
}