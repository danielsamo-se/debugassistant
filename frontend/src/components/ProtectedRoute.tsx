import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface Props {
    children: React.ReactNode;
}

export function ProtectedRoute({ children }: Props) {
    const { isAuthenticated, isLoading } = useAuth();
    const location = useLocation();

    // show loading state while auth is being checked
    if (isLoading) {
        return <div className="flex justify-center items-center h-64">Loading...</div>;
    }

    // redirect unauthenticated users to login
    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }

    return <>{children}</>;
}