import type { AnalyzeResponse } from "../types";

interface Props {
    result: AnalyzeResponse;
}

export default function ResultDisplay({ result }: Props) {
    return (
        <div className="result-section">
            <h3>Analysis Result</h3>

            <div className="summary">
                <p><strong>Lang:</strong> {result.language}</p>
                <p><strong>Type:</strong> {result.exceptionType}</p>
                <p><strong>Score:</strong> {result.score}</p>
            </div>

            {/* Debug JSON View */}
            <pre className="json-dump">
                {JSON.stringify(result, null, 2)}
            </pre>
        </div>
    );
}