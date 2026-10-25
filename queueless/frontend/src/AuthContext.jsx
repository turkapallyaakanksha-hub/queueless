import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, tokenStore } from './api.js';

const AuthContext = createContext(null);
const USER_KEY = 'queueless_user';

function readStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY));
  } catch {
    return null;
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser);

  const save = useCallback((auth) => {
    tokenStore.set(auth.token);
    localStorage.setItem(USER_KEY, JSON.stringify(auth.user));
    setUser(auth.user);
    return auth.user;
  }, []);

  const logout = useCallback(() => {
    tokenStore.clear();
    localStorage.removeItem(USER_KEY);
    setUser(null);
  }, []);

  // api.js fires this event when the server rejects the saved token (expired, for example).
  useEffect(() => {
    window.addEventListener('queueless:unauthorized', logout);
    return () => window.removeEventListener('queueless:unauthorized', logout);
  }, [logout]);

  const value = useMemo(
    () => ({
      user,
      isAdmin: user?.role === 'ADMIN',
      login: async (email, password) =>
        save(await api('/api/auth/login', { method: 'POST', body: { email, password } })),
      register: async (name, email, password) =>
        save(await api('/api/auth/register', { method: 'POST', body: { name, email, password } })),
      logout,
    }),
    [user, save, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}
