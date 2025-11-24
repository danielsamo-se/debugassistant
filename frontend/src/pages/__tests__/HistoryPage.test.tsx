import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { HistoryPage } from '../HistoryPage';
import { getHistory } from '../../services/historyService';

vi.mock('../../services/historyService');

const mockHistoryData = [
  {
    id: '1',
    searchedAt: '2023-10-10T10:00:00Z',
    language: 'Python',
    exceptionType: 'ValueError',
    stackTraceSnippet: 'Traceback (most recent call last)...',
  },
  {
    id: '2',
    searchedAt: '2023-10-11T14:30:00Z',
    language: 'Java',
    exceptionType: 'NullPointerException',
    stackTraceSnippet: 'at com.example.Main.main(Main.java:10)',
  },
];

describe('HistoryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderHistoryPage = () => {
    return render(
      <MemoryRouter>
        <HistoryPage />
      </MemoryRouter>,
    );
  };

  it('should show loading state initially', () => {
    (getHistory as any).mockReturnValue(new Promise(() => {}));

    renderHistoryPage();

    expect(screen.getByText(/loading history/i)).toBeInTheDocument();
  });

  it('should render history list after loading', async () => {
    (getHistory as any).mockResolvedValue(mockHistoryData);

    renderHistoryPage();

    await waitFor(() => {
      expect(screen.queryByText(/loading history/i)).not.toBeInTheDocument();
    });

    expect(screen.getByText('Python')).toBeInTheDocument();
    expect(screen.getByText('ValueError')).toBeInTheDocument();
    expect(screen.getByText('Java')).toBeInTheDocument();
    // Checken ob der Stacktrace Snippet da ist
    expect(screen.getByText(/Traceback/)).toBeInTheDocument();
  });

  it('should show empty state when no history exists', async () => {
    (getHistory as any).mockResolvedValue([]);

    renderHistoryPage();

    await waitFor(() => {
      expect(screen.getByText(/no search history yet/i)).toBeInTheDocument();
    });

    expect(
      screen.getByText(/analyze your first stack trace/i),
    ).toBeInTheDocument();
  });

  it('should show error message if API fails', async () => {
    (getHistory as any).mockRejectedValue(new Error('Network error'));

    renderHistoryPage();

    await waitFor(() => {
      expect(screen.getByText(/failed to load history/i)).toBeInTheDocument();
    });
  });
});
