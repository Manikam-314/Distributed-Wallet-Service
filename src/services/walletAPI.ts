import { apiClient } from "./apiClient";
import { API_CONFIG } from "../api/config";
import type { Wallet } from "@/types";

const { WALLET } = API_CONFIG.ENDPOINTS;

export const walletAPI = {
  createWallet: async (userId: number): Promise<string> => {
    const res = await apiClient.post<string>(WALLET.CREATE, { userId });
    return res.data;
  },

  getWalletByUserId: async (userId: number): Promise<Wallet> => {
    const res = await apiClient.get<any>(`${WALLET.BY_USER}/${userId}`);
    const data = res.data;
    return {
      walletId: data.id,
      userId: data.userId,
      balance: data.balance,
      currency: "INR",
      status: "ACTIVE",
      createdAt: new Date().toISOString()
    };
  },

  getBalance: async (walletId: number): Promise<number> => {
    const res = await apiClient.get<number>(`${WALLET.BALANCE}?walletId=${walletId}`);
    return res.data;
  },

  deposit: async (walletId: number, amount: number): Promise<number> => {
    const res = await apiClient.post<number>(WALLET.DEPOSIT, { walletId, amount });
    return res.data;
  },
};
