import { useState } from 'react';

interface Props {
  onAnalyze: (trace: string) => void;
  loading: boolean;
  collapsed?: boolean;
}

export default function StackTraceInput({ onAnalyze, loading, collapsed = false }: Props) {
  const [localTrace, setLocalTrace] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (localTrace.trim()) {
      onAnalyze(localTrace);
    }
  };

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '12px', alignItems: 'flex-end' }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '6px' }}>
        <textarea
          value={localTrace}
          onChange={(e) => setLocalTrace(e.target.value)}
          placeholder="Paste your error stack trace here..."
          disabled={loading}
          spellCheck={false}
          style={{
            width: '100%',
            height: collapsed ? '32px' : '72px',
            overflow: collapsed ? 'hidden' : 'auto',
            fontFamily: 'IBM Plex Mono, monospace',
            fontSize: '12px',
            lineHeight: '1.65',
            color: '#1a1a18',
            background: 'transparent',
            border: 'none',
            borderBottom: '1px solid #c8c8c4',
            borderRadius: 0,
            padding: '4px 0 8px',
            resize: 'none',
            outline: 'none',
          }}
        />
        <span style={{ fontSize: '11px', color: '#a0a09c' }}>
          {localTrace.length} chars
        </span>
      </div>

      <button
        type="submit"
        disabled={loading || !localTrace.trim()}
        style={{
          fontSize: '13px',
          fontWeight: 500,
          color: loading || !localTrace.trim() ? '#a0a09c' : '#ffffff',
          background: loading || !localTrace.trim() ? '#f6f6f4' : '#1a1a18',
          border: 'none',
          padding: '8px 18px',
          borderRadius: '3px',
          cursor: loading || !localTrace.trim() ? 'not-allowed' : 'pointer',
          fontFamily: 'IBM Plex Sans, sans-serif',
          whiteSpace: 'nowrap',
          marginBottom: '20px',
        }}
      >
        {loading ? 'Analyzing...' : 'Analyze'}
      </button>
    </form>
  );
}
