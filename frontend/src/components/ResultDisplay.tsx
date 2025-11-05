import type { AnalyzeResponse } from "../types";

interface Props {
    result: AnalyzeResponse;
}

export default function ResultDisplay({ result }: Props) {
    return (
        <div className="result-section">
            <div className="summary">
                <div className="badge">Language: <strong>{result.language.toUpperCase()}</strong></div>
                <div className="badge">Type: <strong>{result.exceptionType}</strong></div>
                <div className="badge" style={{ color: result.score > 0 ? 'green' : 'gray' }}>
                    Score: <strong>{result.score}</strong>
                </div>
            </div>

            <h3 style={{ marginBottom: '1rem', color: '#334155' }}>
                Found {result.results.length} Solutions
            </h3>

            <div className="results-list">
                {result.results.length === 0 ? (
                    <p style={{ textAlign: 'center', color: '#64748b' }}>
                        No matches found on GitHub.
                    </p>
                ) : (
                    result.results.map((item, index) => {
                        const isTopMatch = index === 0 && item.score > 2;

                        return (
                            <div
                                key={index}
                                className={`result-card ${isTopMatch ? 'top-match' : ''}`}
                            >
                                <a
                                    href={item.url}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="result-title"
                                >
                                    {isTopMatch && "★ "} {item.title}
                                </a>

                                <div className="result-meta">
                                    <span> {item.comments ?? 0} Comments</span>
                                    <span> {item.reactions} Reactions</span>
                                    <span>Score: {item.score.toFixed(1)}</span>
                                </div>
                            </div>
                        );
                    })
                )}
            </div>
        </div>
    );
}