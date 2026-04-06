import type { SearchResult } from '../types';

interface Props {
  result: SearchResult;
  isTopMatch: boolean;
}

export default function ResultCard({ result, isTopMatch }: Props) {
  const isStackOverflow = result.source === 'stackoverflow';
  const reactions = result.reactions ?? 0;

  return (
    <div style={{
      padding: '14px 0',
      borderBottom: '1px solid #e4e4e0',
      display: 'flex',
      gap: '20px',
      alignItems: 'baseline',
    }}>
      <div style={{ flex: 1 }}>
        <a
          href={result.url}
          target="_blank"
          rel="noopener noreferrer"
          style={{
            fontSize: '13px',
            color: '#1a1a18',
            textDecoration: 'none',
            display: 'block',
            marginBottom: '4px',
            lineHeight: '1.5',
          }}
          onMouseEnter={e => (e.currentTarget.style.color = '#d44c2d')}
          onMouseLeave={e => (e.currentTarget.style.color = '#1a1a18')}
        >
          {result.title}
        </a>

        <div style={{ display: 'flex', gap: '12px', alignItems: 'center' }}>
          <span style={{
            fontSize: '11px',
            color: '#a0a09c',
            fontFamily: 'IBM Plex Mono, monospace',
          }}>
            {isStackOverflow ? 'stackoverflow.com' : 'github.com'}
          </span>

          {isStackOverflow && result.isAnswered && (
            <span style={{ fontSize: '11px', color: '#6b6b68' }}>answered</span>
          )}

          {isStackOverflow ? (
            <span style={{ fontSize: '11px', color: '#a0a09c' }}>
              {reactions} votes · {result.answerCount ?? 0} answers
            </span>
          ) : (
            <span style={{ fontSize: '11px', color: '#a0a09c' }}>
              {reactions} reactions
            </span>
          )}
        </div>
      </div>

      <span style={{
        fontSize: '11px',
        color: isTopMatch ? '#d44c2d' : '#a0a09c',
        fontFamily: 'IBM Plex Mono, monospace',
        whiteSpace: 'nowrap',
      }}>
        {(result.score * 100).toFixed(0)}%
      </span>
    </div>
  );
}
