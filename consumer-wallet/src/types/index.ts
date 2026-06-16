export interface AuthResponse {
  token: string;
  id: number;
  name: string;
  email: string;
}

export interface MoneyRequest {
  id: number;
  requesterId: number;
  recipientId: number;
  amount: number;
  status: "PENDING" | "PAID" | "DECLINED";
  message: string;
  extractedDueDate?: string;
  extractedIntent?: string;
  createdAt: string;
}

export interface User {
  id: number;
  name: string;
  email: string;
  mobileNumber?: string;
  avatar?: string;
  verified: boolean;
  createdAt: string;
}

export interface Wallet {
  walletId: number;
  userId: number;
  balance: number;
  currency: string;
  status: "ACTIVE" | "FROZEN" | "CLOSED";
  createdAt: string;
}

export interface Transaction {
  id: number;
  senderWalletId: number;
  receiverWalletId: number;
  amount: number;
  currency: string;
  type: string;
  status: "SUCCESS" | "PENDING" | "FAILED";
  createdAt: string;
  note?: string;
}

export interface TransferRequest {
  senderWalletId: number;
  receiverWalletId: number;
  amount: number;
}
