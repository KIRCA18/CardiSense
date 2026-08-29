import { createContext, useContext, useEffect, useState } from "react";
import * as api from "./mockApi";

const AuthContext = createContext(null);

const STORAGE_KEY = "cardisense.session";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(undefined); // undefined = not yet resolved

  useEffect(() => {
    const stored = window.localStorage.getItem(STORAGE_KEY);
    setUser(stored ? JSON.parse(stored) : null);
  }, []);

  function persist(nextUser) {
    setUser(nextUser);
    if (nextUser) {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(nextUser));
    } else {
      window.localStorage.removeItem(STORAGE_KEY);
    }
  }

  async function login(credentials) {
    const nextUser = await api.login(credentials);
    persist(nextUser);
    return nextUser;
  }

  async function register(details) {
    // Note: intentionally does not persist a session yet — the account
    // isn't usable until verifyEmail() below succeeds.
    return api.register(details);
  }

  async function verifyEmail({ email, code, fullName }) {
    await api.verifyEmail({ email, code });
    const nextUser = { email, fullName };
    persist(nextUser);
    return nextUser;
  }

  function logout() {
    persist(null);
  }

  return (
    <AuthContext.Provider value={{ user, login, register, verifyEmail, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}
