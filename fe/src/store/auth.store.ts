import { create } from "zustand";
import { persist } from "zustand/middleware";
import { Customer } from "@/types";

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
  user: Customer | null;
  role: "CUSTOMER" | "STAFF" | "BRANCH_MANAGER" | "SUPER_ADMIN" | null;
  setTokens: (access: string, refresh: string) => void;
  setUser: (user: Customer, role: AuthState["role"]) => void;
  logout: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      role: null,
      setTokens: (access, refresh) =>
        set({ accessToken: access, refreshToken: refresh }),
      setUser: (user, role) => set({ user, role }),
      logout: () =>
        set({ accessToken: null, refreshToken: null, user: null, role: null }),
      isAuthenticated: () => !!get().accessToken,
    }),
    { name: "auth" }
  )
);
