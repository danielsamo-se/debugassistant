import { useState } from 'react';

interface Props {
  onAnalyze: (trace: string) => void;
  loading: boolean;
}

export default function StackTraceInput({ onAnalyze, loading }: Props) {
  const [localTrace, setLocalTrace] = useState('');

  return (
    <div className="input-section">
      <textarea
        rows={10}
        placeholder="Paste your Stacktrace here..."
        value={localTrace}
        onChange={(e) => setLocalTrace(e.target.value)}
        disabled={loading}
      />
      <button
        onClick={() => onAnalyze(localTrace)}
        disabled={loading || !localTrace.trim()}
      >
        {loading ? 'Analyzing...' : 'Analyze Stacktrace'}
      </button>
    </div>
  );
}
