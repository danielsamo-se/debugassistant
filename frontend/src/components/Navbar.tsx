import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

export function Navbar() {
    const { user, isAuthenticated, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    return (
        <nav className="bg-slate-800 border-b border-slate-700">
            <div className="max-w-6xl mx-auto px-4 py-3 flex justify-between items-center">

                {/* Logo / Home Link */}
                <Link
                    to="/"
                    className="text-xl font-bold text-white hover:text-blue-400 transition-colors"
                >
                    🔍 Debug Assistant
                </Link>

                <div className="flex items-center gap-4">

                    {/* show different menu for logged in users */}
                    {isAuthenticated ? (
                        <>
                            <Link
                                to="/history"
                                className="text-slate-300 hover:text-white transition-colors"
                            >
                                History
                            </Link>

                            <span className="text-slate-400 text-sm hidden sm:inline-block">
                {user?.name || user?.email}
              </span>

                            <button
                                onClick={handleLogout}
                                className="px-3 py-1 text-sm bg-slate-700 hover:bg-slate-600 text-white rounded transition-colors"
                            >
                                Logout
                            </button>
                        </>
                    ) : (
                        <>
                            {/* guest view */}
                            <Link
                                to="/login"
                                className="text-slate-300 hover:text-white transition-colors"
                            >
                                Login
                            </Link>

                            <Link
                                to="/register"
                                className="px-3 py-1 bg-blue-600 hover:bg-blue-700 text-white rounded transition-colors"
                            >
                                Sign Up
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </nav>
    );
}