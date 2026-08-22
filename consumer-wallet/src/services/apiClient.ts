import axios from "axios";
import { API_CONFIG } from "../api/config";

export const API_BASE_URL = API_CONFIG.BASE_URL;

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    "Content-Type": "application/json",
  },
});

// Add interceptor to inject Authorization header if token exists in local storage
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem("consumer_token");
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
}, (error) => {
  return Promise.reject(error);
});

// Global error handling interceptor
let isRedirecting = false;

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    
    if (status === 401 && !isRedirecting && window.location.pathname !== "/auth") {
      isRedirecting = true;
      // Only remove the JWT token — don't destroy persisted zustand state
      // (bank link, UPI PIN, etc. should survive across re-logins)
      localStorage.removeItem("consumer_token");
      // Redirect to auth page
      window.location.href = "/auth";
      // Reset flag after a short delay so future 401s can also trigger
      setTimeout(() => { isRedirecting = false; }, 2000);
    }
    
    // Construct a user-friendly error message
    const message = error.response?.data?.message || error.message || "An unexpected error occurred";
    error.message = message;
    
    return Promise.reject(error);
  }
);
