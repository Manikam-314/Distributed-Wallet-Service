import axios from "axios";
import { API_CONFIG } from "../api/config";

const notifClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: { "Content-Type": "application/json" },
});

// Inject auth token
notifClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("consumer_token");
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export interface InAppNotification {
  id: number;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export const notificationAPI = {
  getUserNotifications: async (email?: string, mobileNumber?: string): Promise<InAppNotification[]> => {
    try {
      const params = new URLSearchParams();
      if (email) params.append("email", email);
      if (mobileNumber) params.append("mobileNumber", mobileNumber);
      const response = await notifClient.get<InAppNotification[]>(`/notifications/user?${params.toString()}`);
      return response.data;
    } catch {
      return []; // silently fail if notification service is down
    }
  },

  markAsRead: async (id: number): Promise<void> => {
    try {
      await notifClient.post(`/notifications/${id}/read`);
    } catch {
      // ignore
    }
  },
};

