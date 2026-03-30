import { create } from 'zustand';
import type { UserRole } from '../types';

interface AuthState {
  token: string | null;
  userId: number | null;
  name: string | null;
  role: UserRole | null;
  isAuthenticated: boolean;
  setAuth: (token: string, userId: number, name: string, role: UserRole) => void;
  logout: () => void;
}

// Restore from localStorage on init
const stored = localStorage.getItem('user');
const initial = stored ? JSON.parse(stored) : {};

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('token'),
  userId: initial.userId ?? null,
  name: initial.name ?? null,
  role: initial.role ?? null,
  isAuthenticated: !!localStorage.getItem('token'),

  setAuth: (token, userId, name, role) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify({ userId, name, role }));
    set({ token, userId, name, role, isAuthenticated: true });
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    set({ token: null, userId: null, name: null, role: null, isAuthenticated: false });
  },
}));
