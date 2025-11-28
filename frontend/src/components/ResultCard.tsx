import type { SearchResult } from '../types';

interface Props {
  result: SearchResult;
  isTopMatch: boolean;
}

export default function ResultCard({ result, isTopMatch }: Props) {
  const isStackOverflow = result.source === 'stackoverflow';

  // choose color based on result source
  const sourceBadgeColor = isStackOverflow
    ? 'bg-orange-900/30 text-orange-400 border-orange-500/30'
    : 'bg-green-900/30 text-green-400 border-green-500/30';

  // mark highest-ranked item
  const cardBorder = isTopMatch
    ? 'border-blue-500 shadow-lg shadow-blue-500/10'
    : 'border-slate-700 hover:border-slate-600';

  return (
    <div
      className={`block p-5 rounded-lg border bg-slate-800 transition-all ${cardBorder}`}
    >
      <div className="flex justify-between items-start mb-3">
        <div className="flex gap-2">
          {/* show source of search result */}
          <span
            className={`px-2 py-0.5 text-xs font-semibold rounded border ${sourceBadgeColor}`}
          >
            {isStackOverflow ? 'Stack Overflow' : 'GitHub'}
          </span>

          {/* only show if SO thread has an accepted answer */}
          {isStackOverflow && result.isAnswered && (
            <span className="px-2 py-0.5 text-xs font-semibold rounded border bg-teal-900/30 text-teal-400 border-teal-500/30">
              Answered
            </span>
          )}

          {/* highlight best match */}
          {isTopMatch && (
            <span className="px-2 py-0.5 text-xs font-semibold rounded border bg-blue-900/30 text-blue-400 border-blue-500/30">
              Top Match
            </span>
          )}
        </div>

        {/* relevance score from ranking service */}
        <span className="text-xs text-slate-500 font-mono">
          {(result.score * 100).toFixed(0)}% Match
        </span>
      </div>

      <a
        href={result.url}
        target="_blank"
        rel="noopener noreferrer"
        className="text-lg font-semibold text-blue-400 hover:text-blue-300 transition-colors block mb-3"
      >
        {result.title}
      </a>

      <div className="flex gap-4 text-sm text-slate-400">
        {isStackOverflow ? (
          <>
            <span>Score: {result.reactions}</span>
            <span>{result.answerCount} Answers</span>
          </>
        ) : (
          <span>{result.reactions} Reactions</span>
        )}
      </div>
    </div>
  );
}
