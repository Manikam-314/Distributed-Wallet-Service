import { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { MobileAppLayout } from "./components/layout/MobileAppLayout";
import { Home } from "./pages/Home";
import { SendMoneyFlow } from "./pages/SendMoneyFlow";
import { RequestMoneyFlow } from "./pages/RequestMoneyFlow";
import { TransactionHistory } from "./pages/history/TransactionHistory";
import { DigitalCards } from "./pages/cards/DigitalCards";
import { Profile } from "./pages/profile/Profile";
import { KYC } from "./pages/KYC";
import { BankLink } from "./pages/BankLink";
import { AddMoney } from "./pages/AddMoney";
import { QrScanner } from "./pages/QrScanner";
import { PersonalDetails } from "./pages/profile/PersonalDetails";
import { SecurityPrivacy } from "./pages/profile/SecurityPrivacy";
import { Settings } from "./pages/profile/Settings";
import { HelpSupport } from "./pages/profile/HelpSupport";
import { AuthPage } from "./pages/auth/AuthPage";
import { useAuthStore } from "./store/authStore";
import { useWalletStore } from "./store/walletStore";
import { walletAPI } from "./services/walletAPI";

export default function App() {
  const { isAuthenticated, user, logout } = useAuthStore();
  const { setWallet, clearWallet } = useWalletStore();
  const [isInitializing, setIsInitializing] = useState(true);

  // Global Frontend Initialization Flow
  useEffect(() => {
    const initializeSession = async () => {
      if (!isAuthenticated || !user) {
        setIsInitializing(false);
        return;
      }

      // Validate the token is still present in localStorage
      const storedToken = localStorage.getItem("consumer_token");
      if (!storedToken) {
        // Token was removed (e.g. by 401 interceptor) — clear auth state
        logout();
        clearWallet();
        setIsInitializing(false);
        return;
      }

      try {
        // Fetch wallet state to keep frontend in sync with backend
        const currentWallet = await walletAPI.getWalletByUserId(user.id);
        setWallet(currentWallet);
      } catch (err: any) {
        const status = err?.response?.status;
        if (status === 401 || status === 403) {
          // Token is truly invalid — force re-auth
          console.error("Auth expired, forcing re-login");
          localStorage.removeItem("consumer_token");
          logout();
          clearWallet();
        } else {
          // Transient error (wallet-service restarting, network blip, etc.)
          // Keep the user logged in — use cached wallet from zustand persist
          console.warn("Wallet fetch failed (transient), using cached state:", err?.message);
        }
      } finally {
        setIsInitializing(false);
      }
    };

    initializeSession();
  }, [isAuthenticated, user?.id]);

  if (isInitializing) {
    return (
      <div className="flex items-center justify-center h-screen w-full bg-background">
        <div className="animate-spin rounded-full h-12 w-12 border-t-2 border-primary"></div>
      </div>
    );
  }

  if (!isAuthenticated) {
    return (
      <Router>
        <Routes>
          <Route path="*" element={<AuthPage />} />
        </Routes>
      </Router>
    );
  }

  return (
    <Router>
      <Routes>
        <Route path="/" element={<MobileAppLayout><Home /></MobileAppLayout>} />
        <Route path="/pay/send" element={<MobileAppLayout><SendMoneyFlow /></MobileAppLayout>} />
        <Route path="/pay/request" element={<MobileAppLayout><RequestMoneyFlow /></MobileAppLayout>} />
        <Route path="/pay/*" element={<MobileAppLayout><SendMoneyFlow /></MobileAppLayout>} />
        <Route path="/history" element={<MobileAppLayout><TransactionHistory /></MobileAppLayout>} />
        <Route path="/cards" element={<MobileAppLayout><DigitalCards /></MobileAppLayout>} />
        <Route path="/profile" element={<MobileAppLayout><Profile /></MobileAppLayout>} />
        
        {/* Advanced Flows outside Main Layout to overlay full screen */}
        <Route path="/kyc" element={<KYC />} />
        <Route path="/bank-link" element={<BankLink />} />
        <Route path="/add-money" element={<AddMoney />} />
        <Route path="/pay/scan" element={<QrScanner />} />
        <Route path="/profile/personal" element={<MobileAppLayout><PersonalDetails /></MobileAppLayout>} />
        <Route path="/profile/security" element={<MobileAppLayout><SecurityPrivacy /></MobileAppLayout>} />
        <Route path="/profile/settings" element={<MobileAppLayout><Settings /></MobileAppLayout>} />
        <Route path="/profile/support" element={<MobileAppLayout><HelpSupport /></MobileAppLayout>} />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  );
}
