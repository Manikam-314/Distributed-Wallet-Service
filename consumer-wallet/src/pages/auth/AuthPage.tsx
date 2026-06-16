import { useState } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { ShieldCheck, Mail, Lock, User as UserIcon, Phone, KeyRound } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { authAPI } from "@/services/authAPI";
import { useAuthStore } from "@/store/authStore";
import { useNavigate } from "react-router-dom";
import { walletAPI } from "@/services/walletAPI";
import { useWalletStore } from "@/store/walletStore";
import type { User } from "@/types";

// Steps: "auth" (login/register form) | "otp" (verify OTP after registration)
type Step = "auth" | "otp";

export function AuthPage() {
  const navigate = useNavigate();
  const { setAuth } = useAuthStore();
  const { setWallet } = useWalletStore();
  const [isLogin, setIsLogin] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState("");
  const [step, setStep] = useState<Step>("auth");
  const [otp, setOtp] = useState("");

  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    mobileNumber: "",
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  // Called after OTP is verified — complete login flow
  const completeLogin = async (email: string, password: string) => {
    const res = await authAPI.login({ email, password });
    
    const user: User = {
      id: res.id,
      name: res.name,
      email: res.email,
      verified: true, 
      createdAt: new Date().toISOString(),
    };

    // Clear old state before setting new user
    useWalletStore.getState().clearWallet();
    
    setAuth(user, res.token);
    localStorage.setItem("consumer_token", res.token);

    try {
      // Fetch the wallet for this specific user
      const walletData = await walletAPI.getWalletByUserId(res.id);
      setWallet(walletData);
    } catch (err) {
      console.warn("Wallet not found, creating one...");
      await walletAPI.createWallet(res.id);
      const walletData = await walletAPI.getWalletByUserId(res.id);
      setWallet(walletData);
    }

    navigate("/");
  };

  const handleAuthSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    // Professional Form Validation
    if (!isLogin) {
      if (formData.name.trim().length < 2) {
        setError("Name must be at least 2 characters long.");
        return;
      }
      if (formData.password.length < 6) {
        setError("Password must be securely at least 6 characters long.");
        return;
      }
      if (!/^\+?[1-9]\d{1,14}$/.test(formData.mobileNumber.replace(/\s+/g, ""))) {
        setError("Please enter a valid mobile number (e.g. +91XXXXXXXXXX).");
        return;
      }
    }

    setIsProcessing(true);

    try {
      if (isLogin) {
        // Login: no OTP needed, go straight in
        await completeLogin(formData.email, formData.password);
      } else {
        // Register: create account, then show OTP step
        await authAPI.register({
          name: formData.name,
          email: formData.email,
          password: formData.password,
          mobileNumber: formData.mobileNumber,
        });
        // Switch to OTP verification step
        setStep("otp");
        setError("OTP sent to your mobile number. Please enter it below.");
      }
    } catch (err: any) {
      const data = err.response?.data;
      const errMsg =
        typeof data === "string"
          ? data
          : data?.error || data?.message || err.message || "Authentication failed.";
      setError(errMsg);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleOtpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setIsProcessing(true);

    try {
      await authAPI.verifyOtp({ mobileNumber: formData.mobileNumber, otp });

      // OTP verified — complete login flow
      const loginRes = await authAPI.login({
        email: formData.email,
        password: formData.password,
      });

      const user: User = {
        id: loginRes.id,
        name: loginRes.name,
        email: loginRes.email,
        verified: true,
        createdAt: new Date().toISOString(),
      };

      setAuth(user, loginRes.token);
      localStorage.setItem("consumer_token", loginRes.token);

      try {
        const walletData = await walletAPI.getWalletByUserId(loginRes.id);
        setWallet(walletData);
      } catch (err) {
        console.warn("Wallet not found, creating one...");
        await walletAPI.createWallet(loginRes.id);
        const walletData = await walletAPI.getWalletByUserId(loginRes.id);
        setWallet(walletData);
      }

      navigate("/kyc");
    } catch (err: any) {
      const data = err.response?.data;
      const errMsg =
        typeof data === "string"
          ? data
          : data?.error || data?.message || err.message || "Invalid or expired OTP.";
      setError(errMsg);
    } finally {
      setIsProcessing(false);
    }
  };

  const handleResendOtp = async (channel: "SMS" | "EMAIL") => {
    setError("");
    setIsProcessing(true);
    try {
      await authAPI.resendOtp({ email: formData.email, channel });
      setError(`OTP resent successfully via ${channel === "SMS" ? "SMS" : "Gmail"}.`);
    } catch (err: any) {
      const data = err.response?.data;
      const errMsg = typeof data === "string" ? data : data?.error || data?.message || err.message || "Failed to resend OTP.";
      setError(errMsg);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <PageTransition className="flex flex-col h-[100dvh] bg-background pt-12 px-5 pb-8 relative z-50">
      <div className="flex flex-col items-center justify-center mb-10 mt-8">
        <div className="w-20 h-20 bg-primary/10 rounded-[28px] flex items-center justify-center mb-6">
          <ShieldCheck size={40} className="text-primary" />
        </div>
        <h1 className="text-3xl font-bold tracking-tight text-foreground">PayVault</h1>
        <p className="text-muted-foreground mt-2">Secure Digital Finance</p>
      </div>

      <div className="bg-surface border border-border rounded-3xl p-6 shadow-sm flex-1 max-h-[580px]">

        {/* ── OTP STEP ── */}
        {step === "otp" ? (
          <>
            <h2 className="text-2xl font-bold mb-2">Verify OTP</h2>
            <p className="text-muted-foreground text-sm mb-6">
              Enter the OTP sent to your registered contact details.
            </p>

            {error && (
              <div className={`p-3 text-sm rounded-xl mb-6 font-medium ${error.includes("sent") || error.includes("successfully") ? "bg-success/10 text-success" : "bg-danger/10 text-danger"}`}>
                {error}
              </div>
            )}

            <form onSubmit={handleOtpSubmit} className="space-y-4">
              <div className="relative">
                <KeyRound className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
                <Input
                  name="otp"
                  type="text"
                  maxLength={6}
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  className="pl-12 text-center tracking-widest text-lg font-bold"
                  placeholder="_ _ _ _ _ _"
                  required
                />
              </div>
              <Button type="submit" className="w-full mt-4" size="lg" disabled={isProcessing}>
                {isProcessing ? "Verifying..." : "Verify & Continue"}
              </Button>
            </form>

            <div className="mt-8 space-y-3">
              <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider text-center">Resend OTP via</p>
              <div className="grid grid-cols-2 gap-3">
                <Button 
                  variant="outline" 
                  size="sm" 
                  onClick={() => handleResendOtp("SMS")} 
                  disabled={isProcessing}
                  className="rounded-xl border-dashed"
                >
                  <Phone size={14} className="mr-2" /> Mobile
                </Button>
                <Button 
                  variant="outline" 
                  size="sm" 
                  onClick={() => handleResendOtp("EMAIL")} 
                  disabled={isProcessing}
                  className="rounded-xl border-dashed"
                >
                  <Mail size={14} className="mr-2" /> Gmail
                </Button>
              </div>
            </div>

            <p className="text-center mt-6 text-sm text-muted-foreground">
              Wrong details?{" "}
              <button
                type="button"
                onClick={() => { setStep("auth"); setError(""); setOtp(""); }}
                className="text-primary font-semibold ml-1 active:scale-95 transition-transform"
              >
                Go Back
              </button>
            </p>
          </>
        ) : (
          /* ── AUTH (LOGIN / REGISTER) STEP ── */
          <>
            <h2 className="text-2xl font-bold mb-6">{isLogin ? "Welcome Back" : "Create Account"}</h2>

            {error && (
              <div className={`p-3 text-sm rounded-xl mb-6 font-medium ${error.includes("successful") ? "bg-success/10 text-success" : "bg-danger/10 text-danger"}`}>
                {error}
              </div>
            )}

            <form onSubmit={handleAuthSubmit} className="space-y-4">
              {!isLogin && (
                <>
                  <div className="relative">
                    <UserIcon className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
                    <Input
                      name="name"
                      value={formData.name}
                      onChange={handleChange}
                      className="pl-12"
                      placeholder="Full Name"
                      required
                    />
                  </div>
                  <div className="relative">
                    <Phone className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
                    <Input
                      name="mobileNumber"
                      type="tel"
                      value={formData.mobileNumber}
                      onChange={handleChange}
                      className="pl-12"
                      placeholder="+91XXXXXXXXXX"
                      required
                    />
                  </div>
                </>
              )}

              <div className="relative">
                <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
                <Input
                  name="email"
                  type="email"
                  value={formData.email}
                  onChange={handleChange}
                  className="pl-12"
                  placeholder="Email Address"
                  required
                />
              </div>

              <div className="relative">
                <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
                <Input
                  name="password"
                  type="password"
                  value={formData.password}
                  onChange={handleChange}
                  className="pl-12"
                  placeholder="Password"
                  required
                />
              </div>

              <Button type="submit" className="w-full mt-4" size="lg" disabled={isProcessing}>
                {isProcessing ? "Processing..." : isLogin ? "Log In" : "Sign Up"}
              </Button>
            </form>

            <p className="text-center mt-6 text-sm text-muted-foreground">
              {isLogin ? "Don't have an account?" : "Already have an account?"}
              <button
                type="button"
                onClick={() => { setIsLogin(!isLogin); setError(""); }}
                className="text-primary font-semibold ml-1 active:scale-95 transition-transform"
              >
                {isLogin ? "Sign Up" : "Log In"}
              </button>
            </p>
          </>
        )}
      </div>
    </PageTransition>
  );
}
