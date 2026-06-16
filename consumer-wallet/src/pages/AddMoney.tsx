import { useState } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { CreditCard, ArrowRight, Landmark, Smartphone, CheckCircle2, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useWalletStore } from "@/store/walletStore";
import { walletAPI } from "@/services/walletAPI";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";

export function AddMoney() {
  const navigate = useNavigate();
  const { wallet, balance } = useWalletStore();
  const [step, setStep] = useState<"amount" | "method" | "processing" | "success">("amount");
  const [amount, setAmount] = useState<string>("");

  const handleDeposit = async () => {
    if (!wallet) return;
    setStep("processing");
    try {
      const newBalance = await walletAPI.deposit(wallet.walletId, parseFloat(amount));
      useWalletStore.getState().setBalance(newBalance);
      setStep("success");
    } catch (e) {
      console.error(e);
      alert("Deposit failed. Please try again.");
      setStep("amount");
    }
  };

  return (
    <PageTransition className="flex flex-col h-[100dvh] bg-background pt-8 px-5 pb-8 relative z-50">
      
      {step === "amount" && (
        <div className="flex flex-col h-full animate-in fade-in slide-in-from-bottom-4 duration-500">
          <div className="flex justify-between items-center mb-10">
            <motion.button 
              whileTap={{ scale: 0.9 }}
              onClick={() => navigate(-1)} 
              className="relative w-10 h-10 rounded-full bg-surface/50 border border-border flex items-center justify-center text-foreground/70"
            >
              <XCircle size={20} />
            </motion.button>
            <h1 className="text-xl font-black tracking-tight">Add Funds</h1>
            <div className="w-10" />
          </div>

          <div className="flex flex-col items-center mb-12">
            <p className="text-xs font-bold text-muted-foreground uppercase tracking-widest mb-2 opacity-60">Your Balance</p>
            <div className="flex items-baseline gap-1 bg-primary/5 px-4 py-1.5 rounded-full border border-primary/10">
              <span className="text-sm font-bold text-primary">₹</span>
              <span className="text-xl font-black text-primary tracking-tight">{balance.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
            </div>
          </div>
          
          <div className="flex-1 flex flex-col items-center justify-center relative -mt-16">
            <div className="flex items-center text-7xl font-black text-foreground tracking-tighter">
              <span className="text-3xl text-muted-foreground/40 mr-2 font-bold">₹</span>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full bg-transparent outline-none text-center placeholder:opacity-20"
                placeholder="0"
                autoFocus
              />
            </div>
            <div className="mt-8 flex gap-2 overflow-x-auto pb-4 no-scrollbar max-w-full px-4">
              {[500, 1000, 2000, 5000].map(val => (
                <motion.button 
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  key={val}
                  onClick={() => setAmount(val.toString())}
                  className="px-5 py-2.5 rounded-2xl bg-surface border border-border/50 text-sm font-bold hover:border-primary/30 hover:bg-primary/5 transition-all whitespace-nowrap"
                >
                  +₹{val.toLocaleString()}
                </motion.button>
              ))}
            </div>
          </div>

          <Button 
            onClick={() => setStep("method")} 
            disabled={!amount || parseFloat(amount) <= 0}
            className="w-full mt-auto h-16 rounded-3xl text-lg font-black shadow-lg shadow-primary/20 group"
            size="lg"
          >
            Choose Method 
            <motion.div
              animate={{ x: [0, 5, 0] }}
              transition={{ repeat: Infinity, duration: 1.5 }}
              className="ml-2"
            >
              <ArrowRight size={22} />
            </motion.div>
          </Button>
        </div>
      )}

      {step === "method" && (
        <div className="flex flex-col h-full animate-in slide-in-from-right-8 duration-500">
          <div className="flex justify-between items-center mb-8">
            <motion.button 
              whileTap={{ scale: 0.9 }}
              onClick={() => setStep("amount")} 
              className="w-10 h-10 rounded-full bg-surface/50 border border-border flex items-center justify-center text-foreground/70"
            >
              <ArrowRight size={20} className="rotate-180" />
            </motion.button>
            <h1 className="text-xl font-black tracking-tight">Payment Method</h1>
            <div className="w-10" />
          </div>
          
          <p className="text-center text-sm font-medium text-muted-foreground mb-8">
            Funding your wallet with <span className="text-foreground font-black">₹{parseFloat(amount).toLocaleString()}</span>
          </p>
          
          <div className="space-y-4">
            <button onClick={handleDeposit} className="group w-full flex items-center justify-between p-5 bg-surface/40 backdrop-blur-xl border border-border/50 rounded-[28px] hover:bg-surface/80 hover:border-primary/30 hover:shadow-xl hover:shadow-primary/5 transition-all">
              <div className="flex items-center gap-5">
                <div className="w-14 h-14 bg-blue-500/10 text-blue-600 rounded-2xl flex items-center justify-center shadow-inner group-hover:scale-110 transition-transform">
                  <Landmark size={28} />
                </div>
                <div className="text-left">
                  <p className="font-black text-[17px] text-foreground leading-tight">HDFC Bank Primary</p>
                  <p className="text-xs text-muted-foreground font-bold tracking-tight opacity-70 mt-1 uppercase">Saved A/C •••• 4821</p>
                </div>
              </div>
              <div className="w-8 h-8 rounded-full bg-border/20 flex items-center justify-center group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                <ArrowRight size={18} />
              </div>
            </button>

            <button onClick={handleDeposit} className="group w-full flex items-center justify-between p-5 bg-surface/40 backdrop-blur-xl border border-border/50 rounded-[28px] hover:bg-surface/80 hover:border-primary/30 hover:shadow-xl hover:shadow-primary/5 transition-all">
              <div className="flex items-center gap-5">
                <div className="w-14 h-14 bg-purple-500/10 text-purple-600 rounded-2xl flex items-center justify-center shadow-inner group-hover:scale-110 transition-transform">
                  <Smartphone size={28} />
                </div>
                <div className="text-left">
                  <p className="font-black text-[17px] text-foreground leading-tight">Pay via UPI</p>
                  <p className="text-xs text-muted-foreground font-bold tracking-tight opacity-70 mt-1 uppercase">GPay, PhonePe, Paytm</p>
                </div>
              </div>
              <div className="w-8 h-8 rounded-full bg-border/20 flex items-center justify-center group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                <ArrowRight size={18} />
              </div>
            </button>
            
            <button onClick={handleDeposit} className="group w-full flex items-center justify-between p-5 bg-surface/40 backdrop-blur-xl border border-border/50 rounded-[28px] hover:bg-surface/80 hover:border-primary/30 hover:shadow-xl hover:shadow-primary/5 transition-all">
              <div className="flex items-center gap-5">
                <div className="w-14 h-14 bg-emerald-500/10 text-emerald-600 rounded-2xl flex items-center justify-center shadow-inner group-hover:scale-110 transition-transform">
                  <CreditCard size={28} />
                </div>
                <div className="text-left">
                  <p className="font-black text-[17px] text-foreground leading-tight">Card Payment</p>
                  <p className="text-xs text-muted-foreground font-bold tracking-tight opacity-70 mt-1 uppercase">Visa, Mastercard, RuPay</p>
                </div>
              </div>
              <div className="w-8 h-8 rounded-full bg-border/20 flex items-center justify-center group-hover:bg-primary/10 group-hover:text-primary transition-colors">
                <ArrowRight size={18} />
              </div>
            </button>
          </div>
        </div>
      )}

      {step === "processing" && (
        <div className="flex flex-col items-center justify-center h-full">
          <div className="w-16 h-16 border-4 border-primary/20 border-t-primary rounded-full animate-spin mb-4"></div>
          <p className="font-medium text-lg">Processing Deposit...</p>
        </div>
      )}

      {step === "success" && (
        <div className="flex flex-col items-center justify-center h-full text-center animate-in zoom-in-95 duration-500">
          <div className="w-20 h-20 rounded-full bg-success/20 flex flex-col items-center justify-center mb-6 text-success">
            <CheckCircle2 size={40} />
          </div>
          <h2 className="text-3xl font-bold mb-3">Money Added!</h2>
          <p className="text-muted-foreground mb-8">Successfully added ₹{amount} to your PayVault wallet.</p>
          
          <Button variant="outline" className="w-full max-w-xs" onClick={() => navigate("/")}>
            Back to Home
          </Button>
        </div>
      )}
    </PageTransition>
  );
}
