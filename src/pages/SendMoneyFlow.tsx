import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { Search, ArrowRight, CheckCircle2, Lock, AlertCircle } from "lucide-react";
import { v4 as uuidv4 } from "uuid";
import { PageTransition } from "@/components/animations/PageTransition";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useWalletStore } from "@/store/walletStore";
import { useAuthStore } from "@/store/authStore";
import { transactionAPI } from "@/services/transactionAPI";
import { authAPI } from "@/services/authAPI";
import { walletAPI } from "@/services/walletAPI";
import type { User, Transaction } from "@/types";

type Step = "search" | "amount" | "confirm" | "pin" | "processing" | "success" | "failure";

export function SendMoneyFlow() {
  const navigate = useNavigate();
  const { wallet, balance, setBalance } = useWalletStore();
  const { user } = useAuthStore();
  
  const [step, setStep] = useState<Step>("search");
  const [users, setUsers] = useState<User[]>([]);
  const [recipient, setRecipient] = useState<User | null>(null);
  const [amount, setAmount] = useState<string>("");
  const [enteredPin, setEnteredPin] = useState("");
  
  // Fintech architectural state
  const [idempotencyKey, setIdempotencyKey] = useState<string>("");
  const [errorStatus, setErrorStatus] = useState<string | null>(null);
  const [isRetrying, setIsRetrying] = useState(false);
  const pollIntervalRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    authAPI.getUsers()
      .then(allUsers => setUsers(allUsers.filter(u => u.id !== user?.id)))
      .catch((err) => console.error("Failed to load contacts:", err));
  }, [user?.id]);

  // Clean up polling on unmount
  useEffect(() => {
    return () => {
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
    };
  }, []);

  const handleSelectRecipient = (contact: User) => {
    setRecipient(contact);
    setAmount("");
    setErrorStatus(null);
    setStep("amount");
  };

  const handleAmountSubmit = () => {
    const num = parseFloat(amount);
    if (num > 0 && num <= balance) {
      // Generate idempotency key ONCE per transaction attempt
      setIdempotencyKey(`transfer-${uuidv4()}`);
      setStep("confirm");
    } else {
      setErrorStatus("Invalid amount or insufficient balance.");
    }
  };

  const pollTransactionStatus = async (initialTxs: Transaction[]) => {
    if (!wallet) return;
    
    let attempts = 0;
    const maxAttempts = 15; // 15 seconds polling

    pollIntervalRef.current = setInterval(async () => {
      attempts++;
      try {
        const currentTxs = await transactionAPI.getHistory(wallet.walletId);
        
        // Find the newly appeared transaction 
        const newTx = currentTxs.find(tx => 
          !initialTxs.some(old => old.id === tx.id) && 
          tx.amount === parseFloat(amount) && 
          tx.type === "TRANSFER"
        );

        if (newTx) {
          if (newTx.status === "COMPLETED" || newTx.status === "SUCCESS") {
             clearInterval(pollIntervalRef.current!);
             // Fetch verified final balance securely from backend
             const updatedBalance = await walletAPI.getBalance(wallet.walletId);
             setBalance(updatedBalance);
             setStep("success");
          } else if (newTx.status === "FAILED" || newTx.status === "COMPENSATED") {
             clearInterval(pollIntervalRef.current!);
             setErrorStatus("Transaction failed by your bank. Amount not debited.");
             setStep("failure");
          }
        }

        if (attempts >= maxAttempts) {
           clearInterval(pollIntervalRef.current!);
           setErrorStatus("Transaction is taking longer than expected. Please check history later.");
           setStep("failure");
        }
      } catch (err) {
        console.error("Polling error", err);
      }
    }, 1000);
  };

  const handleConfirm = async () => {
    if (!wallet || !recipient || !idempotencyKey) return;
    
    setErrorStatus(null);
    setStep("processing");

    try {
      const recipientWallet = await walletAPI.getWalletByUserId(recipient.id);
      const receiverWalletId = recipientWallet.walletId;
      const parsedAmount = parseFloat(amount);

      // Snapshot history before transfer to detect exact new record
      const initialHistory = await transactionAPI.getHistory(wallet.walletId).catch(() => []);

      await transactionAPI.transfer(
        wallet.walletId,
        receiverWalletId,
        parsedAmount,
        idempotencyKey // Reusable safe key generated during confirm phase
      );

      // Poll the backend distributed ledger instead of optimistic update
      pollTransactionStatus(initialHistory);
      
    } catch (e: any) {
      if (pollIntervalRef.current) clearInterval(pollIntervalRef.current);
      const msg = e.response?.data?.message || e.message || "Network error. Safe to retry.";
      setErrorStatus(msg);
      setStep("failure"); 
    }
  };

  const retryTransaction = () => {
    setIsRetrying(true);
    setEnteredPin("");
    setStep("pin");
    setTimeout(() => setIsRetrying(false), 500);
  };

  return (
    <PageTransition className="flex flex-col h-full w-full max-w-3xl mx-auto bg-background md:pt-8 pt-4 px-4 pb-8 relative shadow-sm overflow-hidden">
      {/* Header */}
      {step !== "success" && step !== "processing" && step !== "failure" && (
        <div className="flex items-center px-4 pb-4 border-b border-border">
          <button onClick={() => step === "search" ? navigate(-1) : setStep("search")} className="p-2 text-primary active:scale-95 transition-transform font-medium">
            Cancel
          </button>
          <h2 className="flex-1 text-center font-semibold text-lg">Send Money</h2>
          <div className="w-14" />
        </div>
      )}

      {/* Global Inline Error Banner */}
      <AnimatePresence>
        {errorStatus && step !== "failure" && (
          <motion.div 
            initial={{ opacity: 0, y: -20 }} 
            animate={{ opacity: 1, y: 0 }} 
            exit={{ opacity: 0, y: -20 }}
            className="m-4 p-3 bg-danger/10 border border-danger/20 text-danger rounded-xl flex items-center gap-2 text-sm font-medium"
          >
            <AlertCircle size={16} />
            {errorStatus}
          </motion.div>
        )}
      </AnimatePresence>

      <div className="flex-1 overflow-y-auto px-4 py-6 relative">
        <AnimatePresence mode="wait">
          {step === "search" && (
            <motion.div key="search" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-6">
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" size={20} />
                <Input className="pl-12 bg-surface" placeholder="Phone number, name, or PVL ID" />
              </div>
              
              <div>
                <h3 className="text-sm font-semibold text-muted-foreground mb-4 uppercase tracking-wider">Recent Contacts</h3>
                <div className="space-y-3">
                  {users.map(contact => (
                    <button 
                      key={contact.id} 
                      onClick={() => handleSelectRecipient(contact)}
                      className="w-full flex items-center gap-4 p-3 rounded-2xl bg-surface border border-border hover:bg-accent active:scale-[0.98] transition-all"
                    >
                      <div className="w-12 h-12 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-lg">
                        {contact.name.charAt(0).toUpperCase()}
                      </div>
                      <div className="text-left flex-1">
                        <p className="font-semibold text-foreground">{contact.name}</p>
                        <p className="text-sm text-muted-foreground">{contact.email}</p>
                      </div>
                    </button>
                  ))}
                </div>
              </div>
            </motion.div>
          )}

          {step === "amount" && recipient && (
            <motion.div key="amount" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="flex flex-col h-full">
              <div className="flex flex-col items-center mt-8">
                <div className="w-16 h-16 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-2xl mb-4">
                  {recipient.name.charAt(0).toUpperCase()}
                </div>
                <h3 className="text-xl font-bold">{recipient.name}</h3>
                <p className="text-muted-foreground text-sm">PVL-{recipient.id.toString().padStart(8, '0')}</p>
              </div>

              <div className="flex-1 flex flex-col items-center justify-center mt-12">
                <div className="flex items-center text-5xl font-bold text-foreground">
                  <span className="text-3xl text-muted-foreground mr-1">₹</span>
                  <input
                    type="number"
                    value={amount}
                    onChange={(e) => {
                       setAmount(e.target.value);
                       setErrorStatus(null);
                    }}
                    className="w-full bg-transparent outline-none text-center"
                    placeholder="0"
                    autoFocus
                  />
                </div>
                <div className="mt-6 py-2 px-4 rounded-full bg-surface border border-border text-sm font-medium">
                  Balance: ₹{balance.toFixed(2)}
                </div>
              </div>

              <Button 
                onClick={handleAmountSubmit} 
                disabled={!amount || parseFloat(amount) <= 0 || parseFloat(amount) > balance}
                className="w-full mt-auto mb-6"
                size="lg"
              >
                Continue <ArrowRight size={18} />
              </Button>
            </motion.div>
          )}

          {step === "confirm" && recipient && (
            <motion.div key="confirm" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, scale: 0.9 }} className="space-y-6">
              <div className="bg-surface rounded-3xl p-6 border border-border shadow-sm text-center">
                <p className="text-muted-foreground mb-2">You are sending</p>
                <h2 className="text-4xl font-bold mb-8">₹{parseFloat(amount).toFixed(2)}</h2>
                
                <div className="flex items-center justify-between p-4 bg-muted/50 rounded-2xl mb-4">
                  <div className="text-left">
                    <p className="text-sm text-muted-foreground">To</p>
                    <p className="font-semibold">{recipient.name}</p>
                  </div>
                  <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center font-bold text-primary">
                    {recipient.name.charAt(0).toUpperCase()}
                  </div>
                </div>

                <div className="flex justify-between text-sm py-3 border-b border-border">
                  <span className="text-muted-foreground">Transaction Fee</span>
                  <span className="font-medium text-success">Free</span>
                </div>
                
                <Button 
                  className="w-full mt-6" 
                  size="lg" 
                  onClick={() => setStep("pin")}
                >
                  Pay Securely
                </Button>
              </div>
            </motion.div>
          )}

          {step === "pin" && recipient && (
            <motion.div key="pin" initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, scale: 0.9 }} className="space-y-6 mt-8 flex flex-col items-center">
              <div className="w-16 h-16 bg-primary/10 rounded-[22px] flex items-center justify-center mb-4 text-primary">
                <Lock size={32} />
              </div>
              <h2 className="text-2xl font-bold mb-2">Enter UPI PIN</h2>
              <p className="text-muted-foreground text-center px-6 mb-6">Enter your 6-digit PIN to authorize the payment of ₹{amount} to {recipient.name}.</p>
              
              <div className="w-full max-w-sm relative">
                 <Input 
                    type="password" 
                    maxLength={6}
                    placeholder="6-Digit PIN" 
                    autoFocus
                    value={enteredPin}
                    onChange={(e) => {
                      setEnteredPin(e.target.value.replace(/\D/g, ''));
                    }}
                    className="text-center text-xl tracking-[1em]"
                  />
              </div>

              <div className="mt-auto pt-10 w-full flex gap-3">
                <Button variant="outline" className="flex-1" onClick={() => setStep("confirm")}>
                  Cancel
                </Button>
                <Button 
                  className="flex-1"
                  disabled={enteredPin.length !== 6 || isRetrying}
                  onClick={handleConfirm}
                >
                   Verify & Send
                </Button>
              </div>
            </motion.div>
          )}

          {step === "processing" && (
            <motion.div key="processing" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="flex flex-col items-center justify-center h-full text-center mt-20">
              <motion.div 
                animate={{ rotate: 360 }}
                transition={{ repeat: Infinity, ease: "linear", duration: 1.5 }}
                className="w-20 h-20 border-4 border-muted border-b-transparent border-t-primary rounded-full mb-8 shadow-sm"
              />
              <h2 className="text-2xl font-bold mb-2 text-foreground">Processing Payment</h2>
              <p className="text-muted-foreground">Please wait while the bank secures your transaction. Do not press back.</p>
            </motion.div>
          )}

          {step === "failure" && (
            <motion.div key="failure" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="flex flex-col items-center justify-center h-full text-center mt-20">
              <div className="w-24 h-24 rounded-full bg-danger/10 flex items-center justify-center mb-6">
                <AlertCircle className="text-danger w-12 h-12" />
              </div>
              <h2 className="text-2xl font-bold mb-2 text-foreground">Transaction Failed</h2>
              <p className="text-muted-foreground mb-8 text-sm px-4">{errorStatus || "An unexpected error occurred during processing."}</p>
              
              <div className="w-full space-y-3 px-4">
                 <Button onClick={retryTransaction} className="w-full" size="lg">
                   Retry Payment
                 </Button>
                 <Button onClick={() => navigate("/")} variant="outline" className="w-full" size="lg">
                   Back to Home
                 </Button>
              </div>
            </motion.div>
          )}

          {step === "success" && (
             <motion.div key="success" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="flex flex-col items-center justify-center h-full text-center mt-20">
               <motion.div 
                 initial={{ scale: 0 }}
                 animate={{ scale: 1 }}
                 transition={{ type: "spring", stiffness: 200, damping: 15 }}
                 className="w-24 h-24 rounded-full bg-success/10 flex items-center justify-center mb-6"
               >
                 <CheckCircle2 className="text-success w-12 h-12" />
               </motion.div>
               <h2 className="text-3xl font-bold mb-2 text-foreground">Payment Sent!</h2>
               <p className="text-muted-foreground mb-8">You successfully sent ₹{amount} to {recipient?.name}.</p>
               
               <div className="bg-surface border border-border rounded-xl p-4 w-full max-w-sm mb-8 flex justify-between">
                  <span className="text-muted-foreground">New Balance</span>
                  <span className="font-semibold">₹{balance.toFixed(2)}</span>
               </div>

               <Button onClick={() => navigate("/")} variant="outline" className="w-full max-w-sm" size="lg">
                 Back to Home
               </Button>
             </motion.div>
          )}
        </AnimatePresence>
      </div>
    </PageTransition>
  );
}
