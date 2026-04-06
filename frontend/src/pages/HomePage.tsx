import { useState } from 'react';
import { analyzeStackTrace } from '../services/analyzeService';
import type { AnalyzeResponse } from '../types';
import StackTraceInput from '../components/StackTraceInput';
import ResultDisplay from '../components/ResultDisplay';
import SkeletonSummary from '../components/skeletons/SkeletonSummary';
import SkeletonResultCard from '../components/skeletons/SkeletonResultCard';

export function HomePage() {
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
    } catch (err) {
      console.error('Analysis request failed', err);
      setError(err instanceof Error ? err.message : 'Analysis failed');
    } finally {
      setIsAnalyzing(false);
    }
  };

  return (
    <div style={{ minHeight: 'calc(100vh - 48px)', background: '#ffffff' }}>
      {/* input */}
      <div
        style={{
          borderBottom: '1px solid #e4e4e0',
          padding: '24px 32px 20px',
        }}
      >
        {!result && !isAnalyzing && (
          <h1
            style={{
              fontSize: '22px',
              fontWeight: 500,
              color: '#1a1a18',
              letterSpacing: '-0.02em',
              marginBottom: '16px',
            }}
          >
            Paste a stack trace
          </h1>
        )}
        <StackTraceInput
          onAnalyze={handleAnalyze}
          loading={isAnalyzing}
          collapsed={!!result}
        />
        {error && (
          <p style={{ marginTop: '10px', fontSize: '13px', color: '#d44c2d' }}>
            {error}
          </p>
        )}
      </div>

      {/* results */}
      {(result || isAnalyzing) && (
        <div
          style={{ display: 'flex', minHeight: 'calc(100vh - 48px - 130px)' }}
        >
          {result && (
            <div
              style={{
                width: '200px',
                minWidth: '200px',
                borderRight: '1px solid #e4e4e0',
                padding: '28px 24px',
                display: 'flex',
                flexDirection: 'column',
                gap: '22px',
              }}
            >
              <MetaGroup label="Language" value={result.language} />
              <MetaGroup
                label="Exception"
                value={result.exceptionType}
                mono
                accent
              />
              <button
                onClick={() => setResult(null)}
                style={{
                  fontSize: '12px',
                  color: '#6b6b68',
                  background: 'none',
                  border: '1px solid #e4e4e0',
                  padding: '5px 10px',
                  borderRadius: '3px',
                  cursor: 'pointer',
                  fontFamily: 'IBM Plex Sans, sans-serif',
                  marginTop: 'auto',
                  textAlign: 'left',
                }}
              >
                New search
              </button>
            </div>
          )}

          <div style={{ flex: 1, padding: '36px 48px', maxWidth: '800px' }}>
            {isAnalyzing && (
              <div
                style={{
                  opacity: 0.5,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '24px',
                }}
              >
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
  );
}

function MetaGroup({
  label,
  value,
  mono = false,
  accent = false,
}: {
  label: string;
  value: string;
  mono?: boolean;
  accent?: boolean;
}) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
      <span
        style={{
          fontSize: '11px',
          textTransform: 'uppercase',
          letterSpacing: '0.07em',
          color: '#a0a09c',
        }}
      >
        {label}
      </span>
      <span
        style={{
          fontSize: mono ? '11px' : '13px',
          fontWeight: mono ? 400 : 500,
          color: accent ? '#d44c2d' : '#1a1a18',
          fontFamily: mono
            ? 'IBM Plex Mono, monospace'
            : 'IBM Plex Sans, sans-serif',
          wordBreak: 'break-word',
        }}
      >
        {value}
      </span>
    </div>
  );
}
