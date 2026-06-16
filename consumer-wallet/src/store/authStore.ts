import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User } from "@/types";

import { useWalletStore } from "./walletStore";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  pin: string;
  setAuth: (user: User, token: string) => void;
  setVerified: (verified: boolean) => void;
  setPin: (pin: string) => void;
  updateKyc: (status: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      pin: "", // No default PIN for new accounts
      setAuth: (user, token) => {
        set({ user, token, isAuthenticated: true });
      },
      setVerified: (verified) => set((state) => ({ user: state.user ? { ...state.user, verified } : null })),
      setPin: (pin) => set({ pin }),
      updateKyc: (status) => set((state) => ({ user: state.user ? { ...state.user, kycStatus: status } : null })),
      logout: () => {
        localStorage.removeItem("consumer_token");
        useWalletStore.getState().clearWallet();
        set({ user: null, token: null, isAuthenticated: false, pin: "" });
      },
    }),
    {
      name: "auth-storage",
    }
  )
);
