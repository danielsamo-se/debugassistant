import { useState } from 'react';

interface Props {
  onAnalyze: (trace: string) => void;
  loading: boolean;
}

export default function StackTraceInput({ onAnalyze, loading }: Props) {
  const [localTrace, setLocalTrace] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (localTrace.trim()) {
      onAnalyze(localTrace);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="flex flex-col h-full group">
      {/* Editor Area */}
      <div className="relative flex-grow bg-zinc-950/30">
        <textarea
          value={localTrace}
          onChange={(e) => setLocalTrace(e.target.value)}
          placeholder="// Paste your error stack trace here..."
          className="w-full h-full p-5 bg-transparent text-zinc-300 font-mono text-sm resize-none focus:outline-none placeholder-zinc-700 leading-relaxed"
          spellCheck={false}
          disabled={loading}
        />
      </div>

      {/* Toolbar / Footer */}
      <div className="border-t border-zinc-800/50 bg-zinc-900/50 p-4 flex justify-between items-center shrink-0 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <span className="text-xs text-zinc-500 font-mono">
            {localTrace.length} chars
          </span>
        </div>

        <button
          type="submit"
          disabled={loading || !localTrace.trim()}
          className={`
            px-5 py-2 text-sm font-semibold rounded shadow-lg transition-all transform active:scale-95
            ${
              loading || !localTrace.trim()
                ? 'bg-zinc-800 text-zinc-600 cursor-not-allowed border border-zinc-700'
                : 'bg-zinc-100 text-zinc-900 hover:bg-white hover:shadow-zinc-500/10 border border-transparent'
            }
          `}
        >
          {loading ? (
            <span className="flex items-center gap-2">
              <svg
                className="animate-spin h-4 w-4 text-zinc-500"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                ></circle>
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                ></path>
              </svg>
              Analyzing...
            </span>
          ) : (
            'Analyze Trace'
          )}
        </button>
      </div>
    </form>
  );
}
