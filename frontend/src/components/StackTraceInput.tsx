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
    <form onSubmit={handleSubmit} className="space-y-4">
      <div className="relative">
        <textarea
          value={localTrace}
          onChange={(e) => setLocalTrace(e.target.value)}
          placeholder="Paste your error stack trace here..."
          className="w-full h-64 p-4 bg-slate-800 border border-slate-700 rounded-lg
                     focus:ring-2 focus:ring-blue-500 focus:border-transparent outline-none
                     text-slate-300 font-mono text-sm resize-y transition-all"
          spellCheck={false}
          disabled={loading}
        />
      </div>

      <button
        type="submit"
        disabled={loading || !localTrace.trim()}
        className={`w-full py-4 rounded-lg font-semibold text-white transition-all
          ${
            loading || !localTrace.trim()
              ? 'bg-slate-700 cursor-not-allowed opacity-50'
              : 'bg-blue-600 hover:bg-blue-700 shadow-lg shadow-blue-500/20 hover:transform hover:-translate-y-0.5'
          }`}
      >
        {loading ? (
          <span className="flex items-center justify-center gap-2">
            <span className="animate-spin"></span> Analyzing
          </span>
        ) : (
          'Analyze Stack Trace'
        )}
      </button>
    </form>
  );
}
