import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { saveHistory } from '../services/historyService';
import { analyzeStackTrace } from '../services/analyzeService';
import StackTraceInput from '../components/StackTraceInput';
import ResultDisplay from '../components/ResultDisplay';

import SkeletonSummary from '../components/skeletons/SkeletonSummary';
import SkeletonResultCard from '../components/skeletons/SkeletonResultCard';

export function HomePage() {
  const { isAuthenticated } = useAuth();

  const [result, setResult] = useState<any>(null);
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
            language: analysisResult.language || 'Unknown',
            exceptionType: analysisResult.exceptionType || 'Unknown Error',
          });
        } catch (saveErr) {
          console.error('Failed to save history:', saveErr);
        }
      }
    } catch (err) {
      console.error('Analysis request failed', err);
      setError(
        'The analysis service is currently unavailable. Please try again later.',
      );
    } finally {
      setIsAnalyzing(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8 animate-fade-in">
      <div className="text-center space-y-4 pt-4">
        <h1 className="text-5xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-blue-400 to-indigo-500">
          Debug Assistant
        </h1>

        <p className="text-slate-400 text-lg max-w-2xl mx-auto">
          Paste your stack trace below. The system will analyze the structure
          and find solutions.
        </p>
      </div>

      <div className="bg-slate-800/50 p-6 rounded-xl border border-slate-700 backdrop-blur-sm">
        <StackTraceInput onAnalyze={handleAnalyze} loading={isAnalyzing} />
      </div>

      {error && (
        <div className="p-4 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400">
          {error}
        </div>
      )}

      {isAnalyzing && !result && (
        <div className="space-y-6 mt-6">
          <SkeletonSummary />
          <SkeletonResultCard />
          <SkeletonResultCard />
          <SkeletonResultCard />
        </div>
      )}

      {!isAnalyzing && result && (
        <div className="mt-8">
          <ResultDisplay result={result} />
        </div>
      )}
    </div>
  );
}
