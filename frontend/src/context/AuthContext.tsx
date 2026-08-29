import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { DecodedToken, UserRole } from '../types';
import { decodeJwt, mintDevToken } from '../utils/jwt';
import { getStoredToken, setStoredToken, removeStoredToken } from '../services/api';

interface AuthContextType {
  token: string;
  decoded: DecodedToken | null;
  role: UserRole | null;
  subject: string | null;
  isAuthenticated: boolean;
  setToken: (token: string) => void;
  clearToken: () => void;
  quickSwitch: (role: UserRole, subject: string) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setTokenState] = useState<string>(() => getStoredToken() || '');
  const [decoded, setDecoded] = useState<DecodedToken | null>(() => decodeJwt(token));

  const updateToken = useCallback((newToken: string) => {
    const trimmed = newToken.trim();
    if (trimmed) {
      setStoredToken(trimmed);
      setTokenState(trimmed);
      setDecoded(decodeJwt(trimmed));
    } else {
      removeStoredToken();
      setTokenState('');
      setDecoded(null);
    }
  }, []);

  const clearToken = useCallback(() => {
    removeStoredToken();
    setTokenState('');
    setDecoded(null);
  }, []);

  const quickSwitch = useCallback(async (role: UserRole, subject: string) => {
    const minted = await mintDevToken(role, subject);
    updateToken(minted);
  }, [updateToken]);

  // If no token exists on first launch, mint a student token so the user has immediate access
  useEffect(() => {
    if (!token) {
      quickSwitch('STUDENT', 'student-A');
    }
  }, [token, quickSwitch]);

  const role = decoded?.roles && decoded.roles.length > 0 ? decoded.roles[0] : null;
  const subject = decoded?.sub || null;
  const isAuthenticated = Boolean(token && decoded && !decoded.isExpired);

  return (
    <AuthContext.Provider
      value={{
        token,
        decoded,
        role,
        subject,
        isAuthenticated,
        setToken: updateToken,
        clearToken,
        quickSwitch,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
