import type { AnalyzeResponse } from '../types';
import ResultCard from './ResultCard';
import CopyButton from './CopyButton';
import ReactMarkdown from 'react-markdown';
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

      {/* agent analysis */}
      {result.mlAnalysis && (
        <div className="bg-zinc-900/50 rounded-md border border-zinc-800 overflow-hidden">
          <div className="flex justify-between items-center p-3 border-b border-zinc-800">
            <span className="text-zinc-500 text-xs uppercase flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              Agent Analysis
            </span>
            <CopyButton text={result.mlAnalysis} />
          </div>

          {result.toolsUsed && result.toolsUsed.length > 0 && (
            <div className="flex gap-2 flex-wrap px-4 pt-3">
              <span className="text-zinc-600 text-[10px] uppercase tracking-wider self-center">Tools used:</span>
              {result.toolsUsed.map((tool) => (
                <span
                  key={tool}
                  className="px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider rounded-sm bg-emerald-900/30 text-emerald-400 border border-emerald-800"
                >
                  {tool.replace(/_/g, ' ')}
                </span>
              ))}
            </div>
          )}

          <div className="p-4 text-sm">
            <ReactMarkdown
              components={{
                p: ({ children }) => <p className="text-zinc-300 leading-relaxed my-2">{children}</p>,
                strong: ({ children }) => <strong className="text-zinc-200 font-semibold">{children}</strong>,
                em: ({ children }) => <em className="text-zinc-300 italic">{children}</em>,
                h1: ({ children }) => <h1 className="text-zinc-200 text-lg font-bold mt-4 mb-2">{children}</h1>,
                h2: ({ children }) => <h2 className="text-zinc-200 text-base font-bold mt-4 mb-2">{children}</h2>,
                h3: ({ children }) => <h3 className="text-zinc-200 text-sm font-bold mt-3 mb-1">{children}</h3>,
                ul: ({ children }) => <ul className="list-disc list-inside my-2 space-y-1">{children}</ul>,
                ol: ({ children }) => <ol className="list-decimal list-inside my-2 space-y-1">{children}</ol>,
                li: ({ children }) => <li className="text-zinc-300">{children}</li>,
                code: ({ className, children }) => {
                  const isBlock = className?.includes('language-');
                  if (isBlock) {
                    return (
                      <code className="block bg-zinc-800 border border-zinc-700 rounded-md p-3 my-3 text-emerald-400 text-xs overflow-x-auto whitespace-pre">
                        {children}
                      </code>
                    );
                  }
                  return (
                    <code className="bg-zinc-800 text-emerald-400 px-1 py-0.5 rounded text-xs">
                      {children}
                    </code>
                  );
                },
                pre: ({ children }) => <div className="my-3">{children}</div>,
                a: ({ href, children }) => (
                  <a href={href} target="_blank" rel="noopener noreferrer" className="text-blue-400 hover:underline">
                    {children}
                  </a>
                ),
              }}
            >
              {result.mlAnalysis}
            </ReactMarkdown>
          </div>
        </div>
      )}

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
