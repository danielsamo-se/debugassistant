import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getHistory } from '../services/historyService';
import { SearchHistory } from '../types';

export function HistoryPage() {
  const [history, setHistory] = useState<SearchHistory[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    try {
      const data = await getHistory();
      setHistory(data);
    } catch (err) {
      console.error(err);
      setError('Failed to load history');
    } finally {
      setIsLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="text-slate-400">Loading history...</div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-white">Search History</h1>

        <Link
          to="/"
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
        >
          New Search
        </Link>
      </div>

      {error && (
        <div className="mb-4 p-3 bg-red-500/20 border border-red-500 text-red-400 rounded">
          {error}
        </div>
      )}

      {!error && history.length === 0 ? (
        <div className="text-center py-12 bg-slate-800 rounded-lg">
          <p className="text-slate-400 mb-4">No search history yet</p>

          <Link to="/" className="text-blue-400 hover:text-blue-300">
            Analyze your first stack trace →
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {history.map((item) => (
            <div
              key={item.id}
              className="p-4 bg-slate-800 rounded-lg border border-slate-700 hover:border-slate-600 transition-colors"
            >
              <div className="flex justify-between items-start mb-2">
                <div className="flex gap-2">
                  {item.language && (
                    <span className="px-2 py-1 bg-blue-500/20 text-blue-400 text-xs rounded font-medium">
                      {item.language}
                    </span>
                  )}

                  {item.exceptionType && (
                    <span className="px-2 py-1 bg-red-500/20 text-red-400 text-xs rounded font-medium">
                      {item.exceptionType}
                    </span>
                  )}
                </div>

                <span className="text-slate-500 text-sm">
                  {formatDate(item.searchedAt)}
                </span>
              </div>

              <pre className="text-slate-300 text-sm font-mono bg-slate-900 p-3 rounded overflow-x-auto">
                {item.stackTraceSnippet}
              </pre>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
