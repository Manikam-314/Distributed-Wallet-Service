import { apiClient } from "./apiClient";
import { API_CONFIG } from "../api/config";
import type { Transaction } from "@/types";

const { TRANSACTIONS } = API_CONFIG.ENDPOINTS;

export const transactionAPI = {
  getHistory: async (walletId: number): Promise<Transaction[]> => {
    const res = await apiClient.get<Transaction[]>(`${TRANSACTIONS.HISTORY}?walletId=${walletId}`);
    return res.data;
  },

  transfer: async (senderWalletId: number, receiverWalletId: number, amount: number, idempotencyKey: string) => {
    const res = await apiClient.post(TRANSACTIONS.TRANSFER, 
      { senderWalletId, receiverWalletId, amount },
      { headers: { "idempotency-key": idempotencyKey } }
    );
    return res.data;
  }
};
