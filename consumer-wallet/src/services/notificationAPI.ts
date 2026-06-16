import axios from "axios";

const notifClient = axios.create({
  baseURL: "http://localhost:8094",
  timeout: 5000,
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
      const response = await notifClient.get<InAppNotification[]>(`/api/notifications/user?${params.toString()}`);
      return response.data;
    } catch {
      return []; // silently fail if notification service is down
    }
  },

  markAsRead: async (id: number): Promise<void> => {
    try {
      await notifClient.post(`/api/notifications/${id}/read`);
    } catch {
      // ignore
    }
  },
};

