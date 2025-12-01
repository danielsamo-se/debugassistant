import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getHistory } from '../services/historyService';
import { SearchHistory } from '../types';

import SkeletonHistoryList from '../components/skeletons/SkeletonHistoryList';

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

  const formatDate = (dateString: string) =>
    new Date(dateString).toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

  if (isLoading) {
    return (
      <div className="max-w-4xl mx-auto animate-fade-in mt-6">
        <SkeletonHistoryList />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto animate-fade-in">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-white">Search History</h1>

        <Link
          to="/"
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg shadow-lg shadow-blue-500/20 text-sm"
        >
          New Search
        </Link>
      </div>

      {error && (
        <div className="p-3 mb-4 bg-red-500/10 border border-red-500/20 text-red-400 rounded-lg">
          {error}
        </div>
      )}

      {!error && history.length === 0 ? (
        <div className="text-center py-12 bg-slate-800 rounded-xl border border-slate-700">
          <p className="text-slate-400 mb-4">No search history yet</p>
          <Link to="/" className="text-blue-400 hover:text-blue-300">
            Analyze your first stack trace
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {history.map((item) => (
            <div
              key={item.id}
              className="p-4 bg-slate-800 rounded-xl border border-slate-700 hover:border-slate-600 shadow-sm"
            >
              <div className="flex justify-between mb-3">
                <div className="flex gap-2">
                  {item.language && (
                    <span className="px-2 py-0.5 bg-blue-900/30 text-blue-400 border border-blue-500/30 text-xs rounded font-medium">
                      {item.language}
                    </span>
                  )}

                  {item.exceptionType && (
                    <span className="px-2 py-0.5 bg-red-900/30 text-red-400 border border-red-500/30 text-xs rounded font-medium">
                      {item.exceptionType}
                    </span>
                  )}
                </div>

                <span className="text-xs text-slate-500 font-mono">
                  {formatDate(item.searchedAt)}
                </span>
              </div>

              <pre className="text-slate-300 text-xs font-mono bg-slate-900/50 p-3 rounded-lg border border-slate-700/50 overflow-x-auto">
                {item.stackTraceSnippet}
              </pre>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
