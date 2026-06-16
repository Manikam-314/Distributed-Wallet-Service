import { apiClient } from "./apiClient";
import { API_CONFIG } from "../api/config";
import type { AuthResponse, User } from "@/types";
import type { LoginRequest, RegisterRequest } from "@/types/api";

const { AUTH } = API_CONFIG.ENDPOINTS;

export const authAPI = {
  login: async (data: LoginRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<AuthResponse>(AUTH.LOGIN, data);
    return res.data;
  },

  register: async (data: RegisterRequest): Promise<string> => {
    const res = await apiClient.post<string>(AUTH.REGISTER, data);
    return res.data;
  },

  verifyOtp: async (data: { mobileNumber: string; otp: string }): Promise<string> => {
    const res = await apiClient.post<string>(AUTH.VERIFY_OTP, data);
    return res.data;
  },

  getUsers: async (): Promise<User[]> => {
    const res = await apiClient.get<User[]>(AUTH.USERS);
    return res.data;
  },

  resendOtp: async (data: { email: string; channel: string }): Promise<string> => {
    const res = await apiClient.post<string>(AUTH.RESEND_OTP, data);
    return res.data;
  }
};

