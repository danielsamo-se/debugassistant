import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function Navbar() {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    // clear local auth state and redirect
    logout();
    navigate('/');
  };

  return (
    <nav className="bg-slate-800 border-b border-slate-700 shadow-lg">
      <div className="max-w-4xl mx-auto px-4 h-16 flex justify-between items-center">
        <Link
          to="/"
          className="text-xl font-bold text-white hover:text-blue-400 transition-colors"
        >
          Debug Assistant
        </Link>

        <div className="flex items-center gap-6">
          {isAuthenticated ? (
            <>
              {/* history is only visible for logged-in users */}
              <Link
                to="/history"
                className="text-slate-300 hover:text-white font-medium transition-colors"
              >
                History
              </Link>

              {/* shows user identity + logout action */}
              <div className="flex items-center gap-4 border-l border-slate-600 pl-6">
                <span className="text-slate-400 text-sm hidden sm:block font-mono">
                  {user?.name || user?.email}
                </span>

                <button
                  onClick={handleLogout}
                  className="px-3 py-1.5 text-sm font-medium bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 rounded-md transition-all"
                >
                  Logout
                </button>
              </div>
            </>
          ) : (
            // guest view (no authentication)
            <div className="flex items-center gap-4">
              <Link
                to="/login"
                className="text-slate-300 hover:text-white font-medium transition-colors"
              >
                Login
              </Link>

              <Link
                to="/register"
                className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg shadow-lg shadow-blue-500/20 transition-all"
              >
                Sign Up
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
