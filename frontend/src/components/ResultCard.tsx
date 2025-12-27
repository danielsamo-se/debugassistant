import type { SearchResult } from '../types';

interface Props {
  result: SearchResult;
  isTopMatch: boolean;
}

export default function ResultCard({ result, isTopMatch }: Props) {
  const isStackOverflow = result.source === 'stackoverflow';
  const reactions = result.reactions ?? 0;

  // choose color based on result source
  const sourceBadgeColor = isStackOverflow
    ? 'bg-orange-500/10 text-orange-400 border-orange-500/20'
    : 'bg-zinc-100 text-zinc-900 border-zinc-200';

  // mark highest-ranked item
  const cardBorder = isTopMatch
    ? 'bg-zinc-900 border-zinc-500 ring-1 ring-zinc-500/50'
    : 'bg-zinc-950 border-zinc-800 hover:border-zinc-600 hover:bg-zinc-900';

  return (
    <div
      className={`group block p-4 rounded-md border transition-all duration-200 ${cardBorder}`}
    >
      <div className="flex justify-between items-start mb-2">
        <div className="flex gap-2 items-center">
          {/* show source of search result */}
          <span
            className={`px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-sm border ${sourceBadgeColor}`}
          >
            {isStackOverflow ? 'Stack Overflow' : 'GitHub'}
          </span>

          {/* only show if SO thread has an accepted answer */}
          {isStackOverflow && result.isAnswered && (
            <span className="px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-sm bg-teal-900/30 text-teal-400 border border-teal-800">
              Answered
            </span>
          )}

          {/* highlight best match */}
          {isTopMatch && (
            <span className="px-1.5 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-sm bg-blue-600 text-white">
              Top Match
            </span>
          )}
        </div>

        {/* relevance score from ranking service */}
        <span className="text-xs text-zinc-500 font-mono group-hover:text-zinc-300">
          {(result.score * 100).toFixed(0)}% Match
        </span>
      </div>

      <a
        href={result.url}
        target="_blank"
        rel="noopener noreferrer"
        className="text-sm font-semibold text-zinc-200 group-hover:text-white group-hover:underline decoration-zinc-500 underline-offset-2 block mb-3 leading-snug"
      >
        {result.title}
      </a>

      <div className="flex gap-4 text-xs text-zinc-500 font-mono border-t border-zinc-800/50 pt-2 mt-2">
        {isStackOverflow ? (
          <>
            <span>Score: {reactions}</span>
            <span>{result.answerCount ?? 0} Answers</span>
          </>
        ) : (
          <span>{reactions} Reactions</span>
        )}
      </div>
    </div>
  );
}
