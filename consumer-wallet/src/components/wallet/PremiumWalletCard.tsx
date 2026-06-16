import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { CheckCircle2, Copy, EyeOff } from "lucide-react";
import { Card, CardContent } from "../ui/card";
import { useWalletStore } from "@/store/walletStore";
import { useAuthStore } from "@/store/authStore";

export function PremiumWalletCard() {
  const { user, pin, setPin } = useAuthStore();
  const { wallet, balance } = useWalletStore();
  const [isBalanceVisible, setIsBalanceVisible] = useState(false);
  const [showPinModal, setShowPinModal] = useState(false);
  const [enteredPin, setEnteredPin] = useState("");
  const [pinError, setPinError] = useState("");

  const isNewAccount = !pin;

  const handleCopy = () => {
    if (wallet) {
      navigator.clipboard.writeText(`PVL-${wallet.walletId}`);
    }
  };

  const handlePinSubmit = () => {
    if (isNewAccount) {
      // Setup PIN
      if (enteredPin.length === 6) {
        setPin(enteredPin);
        setShowPinModal(false);
        setEnteredPin("");
        setPinError("");
        setIsBalanceVisible(true);
      } else {
        setPinError("PIN must be 6 digits");
      }
    } else {
      // Verify PIN
      if (enteredPin !== pin) {
        setPinError("Incorrect PIN");
        setEnteredPin("");
      } else {
        setShowPinModal(false);
        setEnteredPin("");
        setPinError("");
        setIsBalanceVisible(true);
      }
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
      <div className="absolute -inset-1 bg-gradient-to-r from-primary to-emerald-400 rounded-[28px] blur-xl opacity-20 animate-pulse" />
      
      <Card glass className="relative border-0 rounded-[24px] overflow-hidden bg-gradient-to-br from-[#1e293b] via-[#0f172a] to-[#1e1b4b] text-white shadow-2xl">
        {/* Animated background decoration */}
        <motion.div 
          animate={{ 
            scale: [1, 1.2, 1],
            rotate: [0, 90, 0],
            opacity: [0.1, 0.2, 0.1]
          }}
          transition={{ duration: 15, repeat: Infinity, ease: "linear" }}
          className="absolute -top-24 -right-24 w-64 h-64 bg-primary rounded-full blur-3xl pointer-events-none" 
        />
        <motion.div 
          animate={{ 
            scale: [1, 1.5, 1],
            x: [0, 50, 0],
            opacity: [0.05, 0.1, 0.05]
          }}
          transition={{ duration: 10, repeat: Infinity, ease: "easeInOut" }}
          className="absolute -bottom-24 -left-24 w-80 h-80 bg-emerald-500 rounded-full blur-3xl pointer-events-none" 
        />
        
        <CardContent className="p-7 relative z-10">
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
            
            {user?.verified && (
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
                <div className="flex justify-center gap-3 mb-8">
                  {[...Array(6)].map((_, i) => (
                    <div 
                      key={i} 
                      className={`w-4 h-4 rounded-full border-2 transition-all ${i < enteredPin.length ? "bg-primary border-primary scale-110 shadow-[0_0_8px_rgba(var(--primary),0.5)]" : "border-border"}`} 
                    />
                  ))}
                </div>

                {pinError && <p className="text-danger text-sm text-center mb-6 font-bold animate-shake">{pinError}</p>}
                
                <h2 className="text-xl font-bold mb-1">{isNewAccount ? "Setup Your Transaction PIN" : "Enter Secure PIN"}</h2>
                <p className="text-muted-foreground text-sm mb-8 text-center px-4">
                  {isNewAccount 
                    ? "Create a 6-digit PIN to secure your payments and hide balances." 
                    : "Enter your PIN to view your balance safely."}
                </p>
                
                {/* Custom Numeric Keypad */}
                <div className="grid grid-cols-3 gap-4 w-full max-w-xs mb-8">
                  {[1, 2, 3, 4, 5, 6, 7, 8, 9, "clear", 0, "back"].map((val, i) => (
                    <motion.button
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.9 }}
                      key={i}
                      onClick={() => {
                        if (val === "clear") setEnteredPin("");
                        else if (val === "back") setEnteredPin(prev => prev.slice(0, -1));
                        else if (typeof val === "number" && enteredPin.length < 6) setEnteredPin(prev => prev + val);
                        setPinError("");
                      }}
                      className={`h-16 rounded-2xl flex items-center justify-center text-xl font-black ${typeof val === "number" ? "bg-surface border border-border/50 text-foreground shadow-sm" : "text-muted-foreground"}`}
                    >
                      {val === "clear" ? "C" : val === "back" ? "⌫" : val}
                    </motion.button>
                  ))}
                </div>

                <div className="w-full flex gap-3 pb-8">
                  <button 
                    className="flex-1 py-4 rounded-2xl border border-border text-foreground font-bold hover:bg-muted transition-colors active:scale-95" 
                    onClick={() => { setShowPinModal(false); setEnteredPin(""); }}
                  >
                    Cancel
                  </button>
                  <button 
                    className="flex-1 py-4 rounded-2xl bg-primary text-primary-foreground font-bold shadow-lg shadow-primary/20 active:scale-95 transition-all disabled:opacity-30"
                    disabled={enteredPin.length !== 6}
                    onClick={handlePinSubmit}
                  >
                    {isNewAccount ? "Create PIN" : "Confirm PIN"}
                  </button>
                </div>
              </motion.div>
            </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
