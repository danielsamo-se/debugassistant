import { describe, it, expect, vi, beforeEach } from 'vitest';
import { login, register } from '../authService';
import { api } from '../api';

vi.mock('../api', () => ({
    api: {
        post: vi.fn(),
    },
}));

describe('authService', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('login', () => {
        it('should return auth response on success', async () => {
            const mockResponse = {
                token: 'jwt-token',
                expiresIn: 86400000,
                email: 'test@example.com',
                name: 'Test User',
            };

            (api.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
                data: mockResponse
            });

            const result = await login('test@example.com', 'password123');

            expect(api.post).toHaveBeenCalledWith('/auth/login', {
                email: 'test@example.com',
                password: 'password123',
            });
            expect(result).toEqual(mockResponse);
        });

        it('should throw error on failed login', async () => {
            (api.post as ReturnType<typeof vi.fn>).mockRejectedValueOnce(
                new Error('Invalid credentials')
            );

            await expect(login('test@example.com', 'wrong')).rejects.toThrow('Invalid credentials');
        });
    });

    describe('register', () => {
        it('should return auth response on success', async () => {
            const mockResponse = {
                token: 'jwt-token',
                expiresIn: 86400000,
                email: 'new@example.com',
                name: 'New User',
            };

            (api.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
                data: mockResponse
            });

            const result = await register('new@example.com', 'password123', 'New User');

            expect(api.post).toHaveBeenCalledWith('/auth/register', {
                email: 'new@example.com',
                password: 'password123',
                name: 'New User',
            });
            expect(result).toEqual(mockResponse);
        });

        it('should work without name', async () => {
            const mockResponse = {
                token: 'jwt-token',
                expiresIn: 86400000,
                email: 'new@example.com',
                name: null,
            };

            (api.post as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
                data: mockResponse
            });

            const result = await register('new@example.com', 'password123');

            expect(api.post).toHaveBeenCalledWith('/auth/register', {
                email: 'new@example.com',
                password: 'password123',
                name: undefined,
            });
            expect(result).toEqual(mockResponse);
        });
    });
});