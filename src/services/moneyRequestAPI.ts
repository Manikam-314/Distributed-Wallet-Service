import { apiClient } from './apiClient';
import { API_CONFIG } from "../api/config";

const { REQUESTS } = API_CONFIG.ENDPOINTS;

export interface CreateMoneyRequestDto {
    requesterId: number;
    recipientId: number;
    amount: number;
    message: string;
}

export interface MoneyRequest {
    id: number;
    requesterId: number;
    recipientId: number;
    amount: number;
    status: 'PENDING' | 'PAID' | 'DECLINED';
    message?: string;
    extractedDueDate?: string;
    extractedIntent?: string;
    createdAt: string;
}

export const moneyRequestAPI = {
    createRequest: async (data: CreateMoneyRequestDto) => {
        const response = await apiClient.post<MoneyRequest>(REQUESTS.BASE, data);
        return response.data;
    },
    
    getPendingRequests: async (userId: number) => {
        const response = await apiClient.get<MoneyRequest[]>(`${REQUESTS.PENDING}/${userId}`);
        return response.data;
    },
    
    getSentRequests: async (userId: number) => {
        const response = await apiClient.get<MoneyRequest[]>(`${REQUESTS.BASE}/sent/${userId}`);
        return response.data;
    },
    
    payRequest: async (requestId: number, userId: number) => {
        const response = await apiClient.post(`${REQUESTS.PAY}/${requestId}/pay/${userId}`);
        return response.data;
    },
    
    declineRequest: async (requestId: number, userId: number) => {
        const response = await apiClient.post(`${REQUESTS.DECLINE}/${requestId}/decline/${userId}`);
        return response.data;
    }
};
