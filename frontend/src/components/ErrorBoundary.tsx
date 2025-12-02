import React from 'react';

interface State {
  hasError: boolean;
  error: Error | null;
}

export default class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  State
> {
  constructor(props: any) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error('UI crashed:', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="p-8 text-center space-y-4">
          <h1 className="text-2xl font-bold text-red-400">
            Something went wrong.
          </h1>
          <p className="text-slate-400">
            The interface crashed unexpectedly. Please refresh the page.
          </p>
        </div>
      );
    }

    return this.props.children;
  }
}
