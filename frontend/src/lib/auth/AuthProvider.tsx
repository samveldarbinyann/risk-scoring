import { useCallback, useEffect, useMemo, useState, type ReactNode } from "react";
import {
  login as apiLogin,
  logout as apiLogout,
  refresh as apiRefresh,
  register as apiRegister,
  resendCode as apiResendCode,
  verifyEmail as apiVerifyEmail,
  onSessionExpired,
  setAccessToken,
} from "@/lib/api";
import type { LoginRequest, RegisterRequest, UserView } from "@/lib/types";
import { AuthContext, type AuthContextValue, type AuthStatus } from "@/lib/auth/context";

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserView | null>(null);
  const [status, setStatus] = useState<AuthStatus>("loading");

  // Silent session restore: the access token lives only in memory, so a page
  // reload always starts with none. The httpOnly refresh cookie (if still
  // valid) lets us mint a fresh one without asking the user to log in again.
  useEffect(() => {
    let cancelled = false;

    apiRefresh()
      .then((session) => {
        if (cancelled) return;
        setAccessToken(session.accessToken);
        setUser(session.user);
        setStatus("authenticated");
      })
      .catch(() => {
        if (cancelled) return;
        setAccessToken(null);
        setUser(null);
        setStatus("unauthenticated");
      });

    return () => {
      cancelled = true;
    };
  }, []);

  // Fires when a request gets 401 and the follow-up refresh also fails —
  // the session is truly over, drop the client-side user state.
  useEffect(() => {
    return onSessionExpired(() => {
      setUser(null);
      setStatus("unauthenticated");
    });
  }, []);

  const login = useCallback(async (payload: LoginRequest) => {
    const session = await apiLogin(payload);
    setAccessToken(session.accessToken);
    setUser(session.user);
    setStatus("authenticated");
  }, []);

  const register = useCallback((payload: RegisterRequest) => apiRegister(payload), []);

  const verifyEmail = useCallback(async (email: string, code: string) => {
    const session = await apiVerifyEmail({ email, code });
    setAccessToken(session.accessToken);
    setUser(session.user);
    setStatus("authenticated");
  }, []);

  const resendCode = useCallback((email: string) => apiResendCode({ email }), []);

  const logout = useCallback(async () => {
    // Drop client state first: logout must feel instant and must not leave
    // the UI in an authenticated state even if the network call fails.
    setAccessToken(null);
    setUser(null);
    setStatus("unauthenticated");
    await apiLogout().catch(() => undefined);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ user, status, login, register, verifyEmail, resendCode, logout }),
    [user, status, login, register, verifyEmail, resendCode, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
