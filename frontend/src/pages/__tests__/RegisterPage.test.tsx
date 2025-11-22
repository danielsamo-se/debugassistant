import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { RegisterPage } from '../RegisterPage';
import { AuthContext } from '../../context/AuthContext';

const mockRegister = vi.fn();
const mockNavigate = vi.fn();

vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

const renderRegisterPage = () => {
    const authContextValue = {
        user: null,
        isAuthenticated: false,
        isLoading: false,
        login: vi.fn(),
        register: mockRegister,
        logout: vi.fn(),
    };

    return render(
        <AuthContext.Provider value={authContextValue}>
            <BrowserRouter>
                <RegisterPage />
            </BrowserRouter>
        </AuthContext.Provider>
    );
};

describe('RegisterPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('should render register form', () => {
        renderRegisterPage();

        expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/^password$/i)).toBeInTheDocument();
        expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
    });

    it('should call register on form submit', async () => {
        const user = userEvent.setup();
        mockRegister.mockResolvedValueOnce(undefined);

        renderRegisterPage();

        await user.type(screen.getByLabelText(/name/i), 'Test User');
        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getByLabelText(/^password$/i), 'password123');
        await user.type(screen.getByLabelText(/confirm password/i), 'password123');
        await user.click(screen.getByRole('button', { name: /create account/i }));

        await waitFor(() => {
            expect(mockRegister).toHaveBeenCalledWith('test@example.com', 'password123', 'Test User');
        });
    });

    it('should show error when passwords do not match', async () => {
        const user = userEvent.setup();

        renderRegisterPage();

        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getByLabelText(/^password$/i), 'password123');
        await user.type(screen.getByLabelText(/confirm password/i), 'different');
        await user.click(screen.getByRole('button', { name: /create account/i }));

        await waitFor(() => {
            expect(screen.getByText('Passwords do not match')).toBeInTheDocument();
        });

        expect(mockRegister).not.toHaveBeenCalled();
    });

    it('should show error when password is too short', async () => {
        const user = userEvent.setup();

        renderRegisterPage();

        await user.type(screen.getByLabelText(/email/i), 'test@example.com');
        await user.type(screen.getByLabelText(/^password$/i), '123');
        await user.type(screen.getByLabelText(/confirm password/i), '123');
        await user.click(screen.getByRole('button', { name: /create account/i }));

        await waitFor(() => {
            expect(screen.getByText('Password must be at least 6 characters')).toBeInTheDocument();
        });

        expect(mockRegister).not.toHaveBeenCalled();
    });

    it('should have link to login page', () => {
        renderRegisterPage();

        expect(screen.getByRole('link', { name: /sign in/i })).toHaveAttribute('href', '/login');
    });
});