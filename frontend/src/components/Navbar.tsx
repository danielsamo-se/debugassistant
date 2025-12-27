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
    <nav className="bg-zinc-950 border-b border-zinc-800 z-50">
      <div className="w-full px-6 h-14 flex justify-between items-center">
        <Link
          to="/"
          className="text-lg font-bold font-mono text-white tracking-tight flex items-center gap-2"
        >
          <span className="text-zinc-600">{'>'}</span> Debug Assistant
        </Link>

        <div className="flex items-center gap-6 text-sm">
          {isAuthenticated ? (
            <>
              {/* history is only visible for logged-in users */}
              <Link
                to="/history"
                className="text-zinc-400 hover:text-white font-medium transition-colors"
              >
                History
              </Link>

              {/* shows user identity + logout action */}
              <div className="flex items-center gap-4 border-l border-zinc-800 pl-6">
                <span className="text-zinc-500 text-xs hidden sm:block font-mono">
                  {user?.name || user?.email}
                </span>

                <button
                  onClick={handleLogout}
                  className="text-zinc-400 hover:text-white text-xs uppercase font-bold tracking-wider hover:underline decoration-zinc-600 underline-offset-4"
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
                className="text-zinc-400 hover:text-white font-medium transition-colors"
              >
                Login
              </Link>

              <Link
                to="/register"
                className="px-3 py-1.5 bg-white text-black hover:bg-zinc-200 text-xs font-bold uppercase tracking-wide rounded-sm transition-all"
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
