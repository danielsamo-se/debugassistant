import { useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { saveHistory } from '../services/historyService';
import { analyzeStackTrace } from '../services/analyzeService';

export function HomePage() {
  const { isAuthenticated } = useAuth();

  const [stackTrace, setStackTrace] = useState('');
  const [result, setResult] = useState<any>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [error, setError] = useState('');

  const handleAnalyze = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!stackTrace.trim()) return;

    // start new analysis: reset state
    setIsAnalyzing(true);
    setError('');
    setResult(null);

    try {
      // run analysis
      const analysisResult = await analyzeStackTrace(stackTrace);
      setResult(analysisResult);

      // save history (only when logged in)
      if (isAuthenticated) {
        try {
          await saveHistory({
            stackTraceSnippet: stackTrace.substring(0, 500),
            language: analysisResult.language || 'Unknown',
            exceptionType: analysisResult.exceptionType || 'Unknown Error',
          });
        } catch (saveErr) {
          console.error('Failed to save history:', saveErr);
        }
      }
    } catch (err) {
      console.error(err);
      setError('Analysis failed. Please check your stack trace and try again.');
    } finally {
      setIsAnalyzing(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      {/* page header */}
      <div className="text-center space-y-4">
        <h1 className="text-4xl font-bold text-white">Debug Assistant</h1>
        <p className="text-slate-400 text-lg">
          Paste your stack trace below to analyze the error.
        </p>
      </div>

      {/* analysis form */}
      <form onSubmit={handleAnalyze} className="space-y-4">
        <div className="relative">
          {/* stack trace input */}
          <textarea
            value={stackTrace}
            onChange={(e) => setStackTrace(e.target.value)}
            placeholder="Paste your error stack trace here"
            className="w-full h-64 p-4 bg-slate-800 border border-slate-700 rounded-lg
                       focus:ring-2 focus:ring-blue-500 focus:border-transparent
                       text-slate-300 font-mono text-sm resize-none"
            spellCheck={false}
          />
        </div>

        {/* analyze button */}
        <button
          type="submit"
          disabled={isAnalyzing || !stackTrace.trim()}
          className={`w-full py-4 rounded-lg font-semibold text-white transition-all
            ${
              isAnalyzing || !stackTrace.trim()
                ? 'bg-slate-700 cursor-not-allowed'
                : 'bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-500/20'
            }`}
        >
          {isAnalyzing ? 'Analyzing' : 'Analyze Stack Trace'}
        </button>
      </form>

      {/* error display */}
      {error && (
        <div className="p-4 bg-red-500/10 border border-red-500/50 rounded-lg text-red-400">
          {error}
        </div>
      )}

      {/* analysis result output */}
      {result && (
        <div className="space-y-6 animate-fade-in">
          <div className="p-6 bg-slate-800 rounded-lg border border-slate-700">
            <h2 className="text-xl font-bold mb-4 text-green-400">
              Analysis Result
            </h2>
            <div className="prose prose-invert max-w-none text-slate-300">
              <p>
                <strong>Language:</strong> {result.language}
              </p>
              <p>
                <strong>Exception:</strong> {result.exceptionType}
              </p>

              {/* raw output */}
              <div className="mt-4 p-4 bg-slate-900 rounded font-mono text-sm">
                {JSON.stringify(result, null, 2)}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
