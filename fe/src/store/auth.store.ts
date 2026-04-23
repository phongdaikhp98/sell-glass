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

function setSessionCookie() {
  if (typeof document !== "undefined") {
    document.cookie = "sg_session=1; path=/; max-age=604800; SameSite=Lax";
  }
}

function clearSessionCookie() {
  if (typeof document !== "undefined") {
    document.cookie = "sg_session=; path=/; max-age=0; SameSite=Lax";
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      accessToken: null,
      refreshToken: null,
      user: null,
      role: null,
      setTokens: (access, refresh) => {
        set({ accessToken: access, refreshToken: refresh });
        setSessionCookie();
      },
      setUser: (user, role) => set({ user, role }),
      logout: () => {
        set({ accessToken: null, refreshToken: null, user: null, role: null });
        clearSessionCookie();
      },
      isAuthenticated: () => !!get().accessToken,
    }),
    {
      name: "auth",
      // Không persist accessToken — lấy lại bằng refreshToken khi reload
      partialize: (state) => ({
        refreshToken: state.refreshToken,
        user: state.user,
        role: state.role,
      }),
    }
  )
);
