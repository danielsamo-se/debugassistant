import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { saveHistory } from '../services/historyService';
import { analyzeStackTrace } from '../services/analyzeService';

import type { AnalyzeResponse } from '../types';

import StackTraceInput from '../components/StackTraceInput';
import ResultDisplay from '../components/ResultDisplay';

import SkeletonSummary from '../components/skeletons/SkeletonSummary';
import SkeletonResultCard from '../components/skeletons/SkeletonResultCard';

export function HomePage() {
  const { isAuthenticated } = useAuth();

  const [result, setResult] = useState<AnalyzeResponse | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState('');

  const handleAnalyze = async (traceContent: string) => {
    setIsAnalyzing(true);
    setError('');
    setResult(null);

    try {
      const analysisResult = await analyzeStackTrace(traceContent);
      setResult(analysisResult);

      if (isAuthenticated) {
        try {
          await saveHistory({
            stackTraceSnippet: traceContent.substring(0, 500),
            language: analysisResult.language,
            exceptionType: analysisResult.exceptionType,
            searchUrl: window.location.href,
          });
        } catch (saveErr) {
          console.error('Failed to save history:', saveErr);
        }
      }
    } catch (err) {
      console.error('Analysis request failed', err);
      setError(err instanceof Error ? err.message : 'Analysis failed');
    } finally {
      setIsAnalyzing(false);
    }
  };

  const showEmptyState = !result && !isAnalyzing;

  return (
    <div className="relative min-h-[calc(100vh-3.5rem)] bg-zinc-950 flex flex-col overflow-hidden">
      <div className="absolute inset-0 bg-grid-pattern opacity-[0.15] pointer-events-none" />

      <div
        className={`
          relative z-10 flex flex-col transition-all duration-700 ease-in-out h-full
          ${showEmptyState ? 'max-w-3xl mx-auto w-full justify-center pb-20 px-6' : 'flex-row'}
      `}
      >
        <div
          className={`
            flex flex-col transition-all duration-500
            ${showEmptyState ? 'w-full' : 'lg:w-1/2 w-full border-r border-zinc-800/50 bg-zinc-950/50 backdrop-blur-sm p-6'}
        `}
        >
          <div
            className={`mb-6 ${showEmptyState ? 'text-center space-y-3' : ''}`}
          >
            <h1
              className={`font-bold tracking-tight text-white transition-all duration-500 ${showEmptyState ? 'text-4xl' : 'text-sm uppercase text-zinc-400'}`}
            >
              {showEmptyState ? 'Debug Assistant' : 'Input Trace'}
            </h1>

            {showEmptyState && (
              <p className="text-zinc-400 text-lg">
                Paste your stack trace below. The system will analyze the
                structure and find solutions.
              </p>
            )}
          </div>

          <div
            className={`
                flex-grow flex flex-col min-h-0 rounded-xl overflow-hidden border border-zinc-800 shadow-2xl transition-all duration-500
                ${showEmptyState ? 'h-[400px] bg-zinc-900' : 'h-full bg-zinc-900/50'}
            `}
          >
            <StackTraceInput onAnalyze={handleAnalyze} loading={isAnalyzing} />
          </div>

          {error && (
            <div className="mt-4 p-4 bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg text-sm text-center">
              {error}
            </div>
          )}
        </div>

        {(result || isAnalyzing) && (
          <div className="lg:w-1/2 w-full bg-zinc-950/80 p-6 flex flex-col overflow-hidden animate-in fade-in slide-in-from-right-4 duration-500">
            <div className="mb-4 flex justify-between items-center border-b border-zinc-800 pb-4">
              <h2 className="text-sm font-bold text-zinc-400 uppercase tracking-wider flex items-center gap-2">
                <span
                  className={`w-2 h-2 rounded-full ${isAnalyzing ? 'bg-amber-500 animate-pulse' : 'bg-emerald-500'}`}
                ></span>
                Analysis Report
              </h2>
              {result && (
                <button
                  onClick={() => setResult(null)}
                  className="px-3 py-1 text-xs font-medium text-zinc-400 hover:text-white border border-zinc-700 rounded hover:bg-zinc-800 transition-colors"
                >
                  Start New Search
                </button>
              )}
            </div>

            <div className="flex-grow overflow-y-auto pr-2 custom-scrollbar">
              {isAnalyzing && (
                <div className="space-y-6 opacity-60">
                  <SkeletonSummary />
                  <SkeletonResultCard />
                  <SkeletonResultCard />
                </div>
              )}

              {!isAnalyzing && result && <ResultDisplay result={result} />}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
