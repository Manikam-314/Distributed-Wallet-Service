import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { User } from "@/types";

import { useWalletStore } from "./walletStore";

interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  pin: string;
  isBankLinked: boolean;
  linkedBankName: string;
  linkedAccountNumber: string;
  isUpiPinSet: boolean;
  setAuth: (user: User, token: string) => void;
  setVerified: (verified: boolean) => void;
  setPin: (pin: string) => void;
  linkBank: (bankName: string, accountNumber: string) => void;
  setUpiPinSet: (isSet: boolean) => void;
  updateKyc: (status: string) => void;
  logout: () => void;
  clearAllState: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      token: null,
      isAuthenticated: false,
      pin: "", // No default PIN for new accounts
      isBankLinked: false,
      linkedBankName: "",
      linkedAccountNumber: "",
      isUpiPinSet: false,
      setAuth: (user, token) => {
        set({ user, token, isAuthenticated: true });
      },
      setVerified: (verified) => set((state) => ({ user: state.user ? { ...state.user, verified } : null })),
      setPin: (pin) => set({ pin, isUpiPinSet: pin.length > 0 }),
      linkBank: (bankName, accountNumber) => set({ isBankLinked: true, linkedBankName: bankName, linkedAccountNumber: accountNumber }),
      setUpiPinSet: (isSet) => set({ isUpiPinSet: isSet }),
      updateKyc: (status) => set((state) => ({ user: state.user ? { ...state.user, kycStatus: status } : null })),
      logout: () => {
        // Only clear auth credentials — preserve bank/UPI setup state
        // so the user doesn't have to re-enter bank details on re-login
        localStorage.removeItem("consumer_token");
        useWalletStore.getState().clearWallet();
        set({ user: null, token: null, isAuthenticated: false });
      },
      clearAllState: () => {
        // Full reset — used when explicitly switching accounts
        localStorage.removeItem("consumer_token");
        useWalletStore.getState().clearWallet();
        set({ user: null, token: null, isAuthenticated: false, pin: "", isBankLinked: false, linkedBankName: "", linkedAccountNumber: "", isUpiPinSet: false });
      },
    }),
    {
      name: "auth-storage",
    }
  )
);
