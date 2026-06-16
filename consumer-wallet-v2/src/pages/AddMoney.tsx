import { useState } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { CreditCard, ArrowRight, Landmark, Smartphone, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useWalletStore } from "@/store/walletStore";
import { walletAPI } from "@/services/walletAPI";
import { useNavigate } from "react-router-dom";

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
        <div className="flex flex-col h-full animate-in fade-in">
          <button onClick={() => navigate(-1)} className="text-primary font-medium mb-4 self-start">Cancel</button>
          <h1 className="text-3xl font-bold mb-2">Add Money</h1>
          <p className="text-muted-foreground mb-12">Current Balance: ₹{balance.toFixed(2)}</p>
          
          <div className="flex-1 flex flex-col items-center justify-center">
            <div className="flex items-center text-5xl font-bold text-foreground">
              <span className="text-3xl text-muted-foreground mr-1">₹</span>
              <input
                type="number"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full bg-transparent outline-none text-center"
                placeholder="0"
                autoFocus
              />
            </div>
          </div>

          <Button 
            onClick={() => setStep("method")} 
            disabled={!amount || parseFloat(amount) <= 0}
            className="w-full mt-auto"
            size="lg"
          >
            Continue <ArrowRight size={18} />
          </Button>
        </div>
      )}

      {step === "method" && (
        <div className="flex flex-col h-full animate-in slide-in-from-right-4">
          <button onClick={() => setStep("amount")} className="text-primary font-medium mb-4 self-start">Back</button>
          <h1 className="text-2xl font-bold mb-6">Select Payment Method</h1>
          
          <div className="space-y-4">
            <button onClick={handleDeposit} className="w-full flex items-center justify-between p-4 bg-surface border border-border rounded-2xl active:scale-[0.98] transition-transform">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center">
                  <Landmark size={24} />
                </div>
                <div className="text-left">
                  <p className="font-semibold text-foreground">HDFC Bank</p>
                  <p className="text-sm text-muted-foreground">•••• 4821</p>
                </div>
              </div>
              <ArrowRight className="text-muted-foreground" size={20} />
            </button>

            <button onClick={handleDeposit} className="w-full flex items-center justify-between p-4 bg-surface border border-border rounded-2xl active:scale-[0.98] transition-transform">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 bg-purple-100 text-purple-600 rounded-full flex items-center justify-center">
                  <Smartphone size={24} />
                </div>
                <div className="text-left">
                  <p className="font-semibold text-foreground">UPI App</p>
                  <p className="text-sm text-muted-foreground">GPay, PhonePe, Paytm</p>
                </div>
              </div>
              <ArrowRight className="text-muted-foreground" size={20} />
            </button>
            
            <button onClick={handleDeposit} className="w-full flex items-center justify-between p-4 bg-surface border border-border rounded-2xl active:scale-[0.98] transition-transform">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 bg-emerald-100 text-emerald-600 rounded-full flex items-center justify-center">
                  <CreditCard size={24} />
                </div>
                <div className="text-left">
                  <p className="font-semibold text-foreground">Debit/Credit Card</p>
                  <p className="text-sm text-muted-foreground">Visa, Mastercard, RuPay</p>
                </div>
              </div>
              <ArrowRight className="text-muted-foreground" size={20} />
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
