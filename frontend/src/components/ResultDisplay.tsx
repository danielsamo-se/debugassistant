import type { AnalyzeResponse } from '../types';
import ResultCard from './ResultCard';
import CopyButton from './CopyButton';
import { JSX } from 'react';

interface Props {
  result: AnalyzeResponse;
}

export default function ResultDisplay({ result }: Props): JSX.Element {
  const githubCount = result.results.filter(
    (r) => r.source === 'github',
  ).length;

  const soCount = result.results.filter(
    (r) => r.source === 'stackoverflow',
  ).length;

  return (
    <div className="space-y-6 animate-slide-up pb-8">
      {/* summary section */}
      <div className="bg-zinc-900/50 rounded-md border border-zinc-800 overflow-hidden">
        <div className="grid grid-cols-2 divide-x divide-zinc-800 border-b border-zinc-800">
          <div className="p-3">
            <span className="text-zinc-500 text-xs uppercase block mb-1">
              Language
            </span>
            <strong className="text-zinc-200 font-mono text-sm uppercase">
              {result.language}
            </strong>
          </div>

          <div className="p-3">
            <span className="text-zinc-500 text-xs uppercase block mb-1">
              Type
            </span>
            <strong className="text-red-400 font-mono text-sm break-all">
              {result.exceptionType}
            </strong>
          </div>
        </div>

        {result.rootCause && (
          <div className="p-3 bg-zinc-900">
            <div className="flex justify-between items-center mb-1">
              <span className="text-zinc-500 text-xs uppercase">
                Root Cause
              </span>
              <CopyButton text={result.rootCause} />
            </div>

            <strong className="text-zinc-300 font-mono text-sm break-words font-normal">
              {result.rootCause}
            </strong>
          </div>
        )}
      </div>

      {/* result list header */}
      <div>
        <h3 className="text-sm font-bold text-zinc-200 mb-4 flex items-center gap-2 uppercase tracking-wide">
          Found {result.results.length} Solutions
          <span className="text-xs font-normal text-zinc-500 ml-2 font-mono normal-case">
            ({soCount} Stack Overflow, {githubCount} GitHub)
          </span>
        </h3>

        <div className="space-y-3">
          {result.results.length === 0 ? (
            <div className="text-center p-8 bg-zinc-900/50 rounded-md border border-zinc-800 border-dashed text-zinc-500 text-sm">
              No matches found. Try a different stack trace.
            </div>
          ) : (
            result.results.map((item, index) => (
              <ResultCard
                key={item.url}
                result={item}
                isTopMatch={index === 0 && item.score > 0.5}
              />
            ))
          )}
        </div>
      </div>
    </div>
  );
}
