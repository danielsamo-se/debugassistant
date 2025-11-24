import type { SearchResult } from '../types';

interface Props {
  result: SearchResult;
  isTopMatch: boolean;
}

export default function ResultCard({ result, isTopMatch }: Props) {
  const isStackOverflow = result.source === 'stackoverflow';

  return (
    <div className={`result-card ${isTopMatch ? 'top-match' : ''}`}>
      <div className="result-header">
        <span className={`source-badge ${result.source}`}>
          {isStackOverflow ? '📚 Stack Overflow' : '🐙 GitHub'}
        </span>
        {isStackOverflow && result.isAnswered && (
          <span className="answered-badge">✓ Answered</span>
        )}
      </div>

      <a
        href={result.url}
        target="_blank"
        rel="noopener noreferrer"
        className="result-title"
      >
        {isTopMatch && '★ '}
        {result.title}
      </a>

      <div className="result-meta">
        {isStackOverflow ? (
          <>
            <span>Score: {result.reactions}</span>
            <span>{result.answerCount} Answers</span>
          </>
        ) : (
          <span>{result.reactions} Reactions</span>
        )}
        <span className="relevance">
          Relevance: {(result.score * 100).toFixed(0)}%
        </span>
      </div>
    </div>
  );
}
