export const API_CONFIG = {
  BASE_URL: "http://localhost:8090/api",
  TIMEOUT: 10000,
  ENDPOINTS: {
    AUTH: {
      LOGIN: "/auth/login",
      REGISTER: "/auth/register",
      VERIFY_OTP: "/auth/verify-otp",
      USERS: "/auth/users",
      RESEND_OTP: "/auth/resend-otp"
    },
    WALLET: {
      BALANCE: "/wallet/balance",
      DEPOSIT: "/wallet/deposit",
      CREATE: "/wallet/create",
      BY_USER: "/wallet/by-user",
    },
    TRANSACTIONS: {
      BASE: "/transactions",
      HISTORY: "/transactions",
      TRANSFER: "/transactions/transfer",
    },
    REQUESTS: {
      BASE: "/requests",
      PENDING: "/requests/pending",
      PAY: "/requests",
      DECLINE: "/requests",
    }
  }
};
