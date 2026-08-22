import { apiClient } from "./apiClient";

export interface ChatResponse {
  response: string;
  tool: string | null;
  status: string | null;
  reason: string | null;
  executionTime: string | null;
  requestId: string | null;
  confirmationRequired: boolean;
}

export const agentAPI = {
  chat: async (prompt: string): Promise<ChatResponse> => {
    const res = await apiClient.post<ChatResponse>("/agent/chat", { prompt });
    return res.data;
  },

  confirm: async (prompt: string): Promise<ChatResponse> => {
    const res = await apiClient.post<ChatResponse>("/agent/confirm", { prompt });
    return res.data;
  }
};
