import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { HomePage } from '../HomePage';
import { useAuth } from '../../hooks/useAuth';
import { analyzeStackTrace } from '../../services/analyzeService';
import { saveHistory } from '../../services/historyService';

vi.mock('../../hooks/useAuth');
vi.mock('../../services/analyzeService');
vi.mock('../../services/historyService');

const mockAnalyzeResponse = {
  language: 'Java',
  exceptionType: 'NullPointerException',
  message: 'Null pointer exception at line 10',
  keywords: ['nullpointer', 'java'],
  rootCause: 'NullPointerException',
  results: [],
  mlAnalysis: null,
  toolsUsed: null,
};

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should analyze stack trace but not save history when unauthenticated', async () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: false } as ReturnType<
      typeof useAuth
    >);
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true } as ReturnType<
      typeof useAuth
    >);

    render(<HomePage />);
    const user = userEvent.setup();

    const input = screen.getByPlaceholderText(/paste your error stack trace/i);
    await user.type(input, 'Error: NullPointer...');

    await user.click(screen.getByRole('button', { name: /analyze/i }));

    await waitFor(() => {
      expect(screen.getByText('Analysis Result')).toBeInTheDocument();
    });

    expect(analyzeStackTrace).toHaveBeenCalled();
    expect(saveHistory).not.toHaveBeenCalled();
  });

  it('should save history after analysis when authenticated', async () => {
    vi.mocked(useAuth).mockReturnValue({ isAuthenticated: true } as ReturnType<
      typeof useAuth
    >);
    vi.mocked(analyzeStackTrace).mockResolvedValue({
      ...mockAnalyzeResponse,
      language: 'Python',
      exceptionType: 'ValueError',
      message: 'Value error in script',
      keywords: ['valueerror', 'python'],
      rootCause: 'ValueError',
    });

    render(<HomePage />);
    const user = userEvent.setup();

    const input = screen.getByPlaceholderText(/paste your error stack trace/i);
    await user.type(input, 'Traceback...');

    await user.click(screen.getByRole('button', { name: /analyze/i }));

    await waitFor(() => {
      expect(screen.getByText('Analysis Result')).toBeInTheDocument();
    });

    expect(analyzeStackTrace).toHaveBeenCalled();
    expect(saveHistory).toHaveBeenCalled();
  });
});
