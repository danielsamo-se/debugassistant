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
    <div className="space-y-8 animate-slide-up">
      {/* summary section */}
      <div className="bg-slate-800 p-6 rounded-xl border border-slate-700 shadow-xl">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-700">
            <span className="text-slate-400 text-xs uppercase tracking-wider block mb-1">
              Language
            </span>
            <strong className="text-green-400 font-mono text-lg">
              {result.language.toUpperCase()}
            </strong>
          </div>

          <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-700">
            <span className="text-slate-400 text-xs uppercase tracking-wider block mb-1">
              Type
            </span>
            <strong className="text-red-400 font-mono text-lg break-all">
              {result.exceptionType}
            </strong>
          </div>

          {result.rootCause && (
            <div className="bg-slate-900/50 p-3 rounded-lg border border-slate-700">
              <div className="flex justify-between items-start mb-1">
                <span className="text-slate-400 text-xs uppercase tracking-wider">
                  Root Cause
                </span>
                <CopyButton text={result.rootCause} />
              </div>

              <strong className="text-yellow-400 font-mono text-lg break-all">
                {result.rootCause}
              </strong>
            </div>
          )}
        </div>
      </div>

      {/* result list header */}
      <div>
        <h3 className="text-xl font-bold text-slate-200 mb-4 flex items-center gap-2">
          Found {result.results.length} Solutions
          <span className="text-sm font-normal text-slate-500 ml-2">
            ({soCount} Stack Overflow, {githubCount} GitHub)
          </span>
        </h3>

        <div className="space-y-4">
          {result.results.length === 0 ? (
            <div className="text-center p-8 bg-slate-800/50 rounded-lg border border-slate-700 text-slate-400">
              No matches found. Try a different stack trace.
            </div>
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
    </div>
  );
}
