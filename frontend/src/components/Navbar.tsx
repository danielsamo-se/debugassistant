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
    <nav style={{
      borderBottom: '1px solid #e4e4e0',
      padding: '0 32px',
      height: '48px',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      background: '#ffffff',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '24px' }}>
        <Link to="/" style={{
          fontSize: '13px',
          fontWeight: 500,
          color: '#1a1a18',
          textDecoration: 'none',
          letterSpacing: '-0.01em',
        }}>
          Debug Assistant
        </Link>

        {isAuthenticated && (
          <Link to="/history" style={{
            fontSize: '13px',
            color: '#6b6b68',
            textDecoration: 'none',
          }}>
            History
          </Link>
        )}
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
        {isAuthenticated ? (
          <>
            <span style={{ fontSize: '12px', color: '#a0a09c', fontFamily: 'IBM Plex Mono, monospace' }}>
              {user?.name || user?.email}
            </span>
            <button onClick={handleLogout} style={{
              fontSize: '13px',
              color: '#6b6b68',
              background: 'none',
              border: '1px solid #e4e4e0',
              padding: '5px 12px',
              cursor: 'pointer',
              borderRadius: '3px',
              fontFamily: 'IBM Plex Sans, sans-serif',
            }}>
              Log out
            </button>
          </>
        ) : (
          <>
            <Link to="/login" style={{
              fontSize: '13px',
              color: '#6b6b68',
              textDecoration: 'none',
              padding: '5px 12px',
              border: '1px solid #e4e4e0',
              borderRadius: '3px',
            }}>
              Log in
            </Link>
            <Link to="/register" style={{
              fontSize: '13px',
              fontWeight: 500,
              color: '#ffffff',
              background: '#1a1a18',
              textDecoration: 'none',
              padding: '5px 12px',
              borderRadius: '3px',
            }}>
              Sign up
            </Link>
          </>
        )}
      </div>
    </nav>
  );
}
