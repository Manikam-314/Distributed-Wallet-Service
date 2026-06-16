import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User } from "@/types";

import { useWalletStore } from "./walletStore";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  setAuth: (user: User, token: string) => void;
  updateKyc: (status: User["kycStatus"]) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      setAuth: (user, token) => {
        set({ user, token, isAuthenticated: true });
      },
      updateKyc: (status) => set((state) => ({ user: state.user ? { ...state.user, kycStatus: status } : null })),
      logout: () => {
        localStorage.removeItem("consumer_token");
        useWalletStore.getState().clearWallet();
        set({ user: null, token: null, isAuthenticated: false });
      },
    }),
    {
      name: "auth-storage",
    }
  )
);
