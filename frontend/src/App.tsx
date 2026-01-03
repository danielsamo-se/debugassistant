import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { ProtectedRoute } from './components/ProtectedRoute';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { HistoryPage } from './pages/HistoryPage';
import ErrorBoundary from './components/ErrorBoundary';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <ErrorBoundary>
          <div className="min-h-screen flex flex-col">
            <Navbar />
            <main className="flex-grow w-full max-w-6xl mx-auto px-4 py-8">
              <Routes>
                <Route
                  path="/"
                  element={
                    <ErrorBoundary>
                      <HomePage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/login"
                  element={
                    <ErrorBoundary>
                      <LoginPage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/register"
                  element={
                    <ErrorBoundary>
                      <RegisterPage />
                    </ErrorBoundary>
                  }
                />
                <Route
                  path="/history"
                  element={
                    <ProtectedRoute>
                      <ErrorBoundary>
                        <HistoryPage />
                      </ErrorBoundary>
                    </ProtectedRoute>
                  }
                />
              </Routes>
            </main>
          </div>
        </ErrorBoundary>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
