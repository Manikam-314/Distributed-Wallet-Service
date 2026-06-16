import { create } from "zustand";
import { persist } from "zustand/middleware";
import type { Wallet } from "@/types";
import type { MoneyRequest } from "@/services/moneyRequestAPI";

interface WalletState {
  wallet: Wallet | null;
  balance: number;
  isLoading: boolean;
  pendingRequests: MoneyRequest[];
  setWallet: (wallet: Wallet) => void;
  setBalance: (balance: number) => void;
  setLoading: (loading: boolean) => void;
  setPendingRequests: (requests: MoneyRequest[]) => void;
  removeRequest: (requestId: number) => void;
  clearWallet: () => void;
}

export const useWalletStore = create<WalletState>()(
  persist(
    (set) => ({
      wallet: null,
      balance: 0,
      isLoading: false,
      pendingRequests: [],
      setWallet: (wallet) => set({ wallet, balance: wallet.balance, isLoading: false }),
      setBalance: (balance) => set({ balance }),
      setLoading: (loading) => set({ isLoading: loading }),
      setPendingRequests: (requests) => set({ pendingRequests: requests, isLoading: false }),
      removeRequest: (requestId) => set((state) => ({ 
          pendingRequests: state.pendingRequests.filter(r => r.id !== requestId) 
      })),
      clearWallet: () => set({ wallet: null, balance: 0, isLoading: false, pendingRequests: [] }),
    }),
    {
      name: "wallet-storage",
    }
  )
);
