import type { AnalyzeResponse } from '../types';
import ResultCard from './ResultCard';
import CopyButton from './CopyButton';
import ReactMarkdown from 'react-markdown';
import { JSX } from 'react';

interface Props {
  result: AnalyzeResponse;
}

export default function ResultDisplay({ result }: Props): JSX.Element {
  const githubCount = result.results.filter((r) => r.source === 'github').length;
  const soCount = result.results.filter((r) => r.source === 'stackoverflow').length;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px', paddingBottom: '48px' }}>

      {/* Summary */}
      <div>
        <h1 style={{
          fontSize: '20px',
          fontWeight: 500,
          color: '#1a1a18',
          letterSpacing: '-0.02em',
          marginBottom: '8px',
        }}>
          {result.exceptionType}
        </h1>

        {result.rootCause && (
          <div style={{ display: 'flex', alignItems: 'flex-start', gap: '8px' }}>
            <p style={{ fontSize: '13px', color: '#6b6b68', lineHeight: '1.7', flex: 1, fontFamily: 'IBM Plex Mono, monospace' }}>
              {result.rootCause}
            </p>
            <CopyButton text={result.rootCause} />
          </div>
        )}
      </div>

      {/* Agent analysis */}
      {result.mlAnalysis && (
        <div>
          <div style={{
            display: 'flex',
            justifyContent: 'flex-end',
            alignItems: 'center',
            marginBottom: '16px',
          }}>
            <div style={{ border: '0.5px solid #e4e4e0', borderRadius: '3px', padding: '4px' }}>
              <CopyButton text={result.mlAnalysis} />
            </div>
          </div>

          <div style={{ fontSize: '13px', color: '#6b6b68', lineHeight: '1.7' }}>
            <ReactMarkdown
              components={{
                p: ({ children }) => <p style={{ margin: '0 0 12px', color: '#6b6b68', lineHeight: '1.7' }}>{children}</p>,
                strong: ({ children }) => <strong style={{ color: '#1a1a18', fontWeight: 500 }}>{children}</strong>,
                em: ({ children }) => <em style={{ color: '#6b6b68' }}>{children}</em>,
                h1: ({ children }) => <h1 style={{ fontSize: '15px', fontWeight: 500, color: '#1a1a18', margin: '20px 0 8px' }}>{children}</h1>,
                h2: ({ children }) => <h2 style={{ fontSize: '14px', fontWeight: 500, color: '#1a1a18', margin: '16px 0 6px' }}>{children}</h2>,
                h3: ({ children }) => <h3 style={{ fontSize: '13px', fontWeight: 500, color: '#1a1a18', margin: '12px 0 4px' }}>{children}</h3>,
                ul: ({ children }) => <ul style={{ paddingLeft: '20px', margin: '8px 0' }}>{children}</ul>,
                ol: ({ children }) => <ol style={{ paddingLeft: '20px', margin: '8px 0' }}>{children}</ol>,
                li: ({ children }) => <li style={{ color: '#6b6b68', marginBottom: '4px' }}>{children}</li>,
                code: ({ className, children }) => {
                  const isBlock = className?.includes('language-');
                  if (isBlock) {
                    return (
                      <code style={{
                        display: 'block',
                        fontFamily: 'IBM Plex Mono, monospace',
                        fontSize: '12px',
                        background: '#f6f6f4',
                        borderLeft: '3px solid #c8c8c4',
                        padding: '12px 16px',
                        margin: '12px 0',
                        overflowX: 'auto',
                        whiteSpace: 'pre',
                        color: '#1a1a18',
                      }}>
                        {children}
                      </code>
                    );
                  }
                  return (
                    <code style={{
                      fontFamily: 'IBM Plex Mono, monospace',
                      fontSize: '12px',
                      background: '#f6f6f4',
                      border: '1px solid #e4e4e0',
                      padding: '1px 5px',
                      borderRadius: '2px',
                      color: '#1a1a18',
                    }}>
                      {children}
                    </code>
                  );
                },
                pre: ({ children }) => <div style={{ margin: '12px 0' }}>{children}</div>,
                a: ({ href, children }) => (
                  <a href={href} target="_blank" rel="noopener noreferrer" style={{ color: '#d44c2d', textDecoration: 'none' }}>
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

      {/* Results */}
      <div>
        <div style={{
          borderTop: '1px solid #e4e4e0',
          paddingTop: '20px',
          marginBottom: '4px',
        }}>
          <span style={{
            fontSize: '11px',
            textTransform: 'uppercase',
            letterSpacing: '0.07em',
            color: '#a0a09c',
          }}>
            {result.results.length} results · {soCount} Stack Overflow · {githubCount} GitHub
          </span>
        </div>

        <div>
          {result.results.length === 0 ? (
            <p style={{ fontSize: '13px', color: '#a0a09c', padding: '20px 0' }}>
              No matches found. Try a different stack trace.
            </p>
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
