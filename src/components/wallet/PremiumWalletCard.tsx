import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CheckCircle2, Copy, Lock, EyeOff } from "lucide-react";
import { Card, CardContent } from "../ui/card";
import { useWalletStore } from "@/store/walletStore";
import { useAuthStore } from "@/store/authStore";

export function PremiumWalletCard() {
  const { user, pin } = useAuthStore();
  const { wallet, balance } = useWalletStore();
  const [isBalanceVisible, setIsBalanceVisible] = useState(false);
  const [showPinModal, setShowPinModal] = useState(false);
  const [enteredPin, setEnteredPin] = useState("");
  const [pinError, setPinError] = useState("");

  const handleCopy = () => {
    if (wallet) {
      navigator.clipboard.writeText(`PVL-${wallet.walletId}`);
    }
  };

  const handlePinSubmit = () => {
    if (pin && enteredPin !== pin) {
       setPinError("Incorrect PIN");
       setEnteredPin("");
    } else {
       setShowPinModal(false);
       setEnteredPin("");
       setPinError("");
       setIsBalanceVisible(true);
    }
  };

  return (
    <motion.div
      initial={{ scale: 0.95, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      transition={{ duration: 0.4, ease: "easeOut" }}
      className="relative w-full max-w-sm mx-auto my-4"
    >
      {/* Decorative gradient background glows */}
      <div className="absolute -inset-0.5 bg-gradient-to-r from-primary to-emerald-400 rounded-[24px] blur opacity-30 animate-pulse" />
      
      <Card glass className="relative border-0 rounded-[22px] overflow-hidden bg-gradient-to-br from-primary/90 to-indigo-900/90 text-white shadow-2xl">
        <CardContent className="p-6">
          <div className="flex justify-between items-start mb-8">
            <div>
              <p className="text-white/70 text-sm font-medium mb-1">Total Balance</p>
              {!isBalanceVisible ? (
                <button 
                  onClick={() => setShowPinModal(true)}
                  className="mt-2 text-white font-bold text-lg flex items-center gap-2 tracking-wide active:scale-95 transition-transform bg-white/10 hover:bg-white/20 px-4 py-2 rounded-full backdrop-blur-md border border-white/20"
                >
                  Check Balance
                </button>
              ) : (
                <div className="flex items-baseline gap-1 mt-1">
                  <span className="text-2xl font-semibold opacity-90">₹</span>
                  <span className="text-4xl font-bold tracking-tight">{balance.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
                  <button onClick={() => setIsBalanceVisible(false)} className="ml-3 p-1 rounded-full hover:bg-white/10 text-white/60 hover:text-white transition-colors">
                    <EyeOff size={18} />
                  </button>
                </div>
              )}
            </div>
            
            {user?.kycStatus === "VERIFIED" && (
              <div className="flex items-center gap-1 bg-white/20 px-2 py-1 rounded-full text-xs font-medium backdrop-blur-md">
                <CheckCircle2 size={14} className="text-emerald-300" />
                Verified
              </div>
            )}
          </div>

          {/* Card Details */}
          <div className="mt-8 flex justify-between items-end">
            <div className="flex flex-col">
              <span className="text-xs text-white/60 mb-1 uppercase tracking-wider font-semibold">Cardholder</span>
              <span className="font-medium tracking-wide">{user ? user.name : "Loading..."}</span>
            </div>
            
            <button 
              onClick={handleCopy}
              className="text-left group active:scale-95 transition-transform"
            >
              <span className="text-xs text-white/60 mb-1 uppercase tracking-wider font-semibold flex items-center gap-1">
                Wallet ID <Copy size={12} className="opacity-0 group-hover:opacity-100 transition-opacity" />
              </span>
              <span className="font-medium tracking-wide flex items-center gap-1">
                PVL - {wallet ? String(wallet.walletId).padStart(4, '0') : "XXXX"}
              </span>
            </button>
          </div>
        </CardContent>
      </Card>
      <AnimatePresence>
        {showPinModal && (
          <motion.div 
            initial={{ opacity: 0 }} 
            animate={{ opacity: 1 }} 
            exit={{ opacity: 0 }} 
            className="fixed inset-0 z-50 flex flex-col justify-end bg-black/60 backdrop-blur-sm"
          >
            <motion.div 
              initial={{ y: "100%" }} 
              animate={{ y: 0 }} 
              exit={{ y: "100%" }} 
              transition={{ type: "spring", damping: 25, stiffness: 200 }}
              className="bg-surface w-full rounded-t-3xl min-h-[50vh] p-6 flex flex-col items-center"
            >
              <div className="w-12 h-1.5 bg-border rounded-full mb-6" />
              <div className="w-16 h-16 bg-primary/10 rounded-[22px] flex items-center justify-center mb-4 text-primary">
                <Lock size={32} />
              </div>
              <h2 className="text-2xl font-bold mb-2 text-foreground">Check Balance</h2>
              <p className="text-muted-foreground text-center px-6 mb-6">Enter your 6-digit UPI PIN to view your wallet balance.</p>
              
              <div className="w-full max-w-sm relative">
                <input 
                  type="password" 
                  maxLength={6}
                  placeholder="6-Digit PIN" 
                  autoFocus
                  value={enteredPin}
                  onChange={(e) => {
                    setEnteredPin(e.target.value.replace(/\D/g, ''));
                    setPinError("");
                  }}
                  className={`w-full bg-background border ${pinError ? "border-danger" : "border-border"} rounded-xl p-4 text-center text-2xl tracking-[1em] text-foreground focus:outline-none focus:ring-2 ${pinError ? "focus:ring-danger" : "focus:ring-primary/50"}`}
                />
                {pinError && <p className="text-danger text-sm text-center mt-3 font-medium">{pinError}</p>}
              </div>

              <div className="mt-8 w-full flex gap-3 pb-8">
                <button 
                  className="flex-1 py-3.5 rounded-xl border border-border text-foreground font-semibold active:scale-95 transition-transform" 
                  onClick={() => setShowPinModal(false)}
                >
                  Cancel
                </button>
                <button 
                  className="flex-1 py-3.5 rounded-xl bg-primary text-primary-foreground font-semibold active:scale-95 transition-transform disabled:opacity-50"
                  disabled={enteredPin.length !== 6}
                  onClick={handlePinSubmit}
                >
                  View Balance
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
