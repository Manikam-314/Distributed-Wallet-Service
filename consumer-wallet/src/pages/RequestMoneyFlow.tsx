import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";
import { Search, ArrowRight, CheckCircle2 } from "lucide-react";
import { PageTransition } from "@/components/animations/PageTransition";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useWalletStore } from "@/store/walletStore";
import { useAuthStore } from "@/store/authStore";
import { moneyRequestAPI } from "@/services/moneyRequestAPI";
import { authAPI } from "@/services/authAPI";
// import { walletAPI } from "@/services/walletAPI";
import type { User } from "@/types";

export function RequestMoneyFlow() {
  const navigate = useNavigate();
  const { wallet } = useWalletStore();
  const { user } = useAuthStore();
  
  const [step, setStep] = useState<"search" | "amount" | "success">("search");
  const [users, setUsers] = useState<User[]>([]);
  const [recipient, setRecipient] = useState<User | null>(null);
  const [amount, setAmount] = useState<string>("");
  const [message, setMessage] = useState<string>("");
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    authAPI.getUsers()
      .then(allUsers => setUsers(allUsers.filter(u => u.id !== user?.id)))
      .catch(console.error);
  }, [user?.id]);

  const handleSelectRecipient = (contact: User) => {
    setRecipient(contact);
    setStep("amount");
  };

  const handleRequestSubmit = async () => {
    const num = Number(amount);
    
    if (!wallet) {
      alert("Error: Your wallet information is missing. Please log out and log back in to refresh your session state.");
      return;
    }
    
    if (!recipient || num <= 0) {
      alert("Error: Please specify a valid recipient and amount greater than 0.");
      return;
    }
    
    setIsProcessing(true);

    try {
      await moneyRequestAPI.createRequest({
        requesterId: user!.id,
        recipientId: recipient.id,
        amount: num,
        message: message || "Please pay me"
      });

      setStep("success");
    } catch (e: any) {
      console.error(e);
      const data = e.response?.data;
      const msg = data?.error || data?.message || e.message || "Failed to create request";
      alert(msg);
    } finally {
      setIsProcessing(false);
    }
  };

  return (
    <PageTransition className="flex flex-col h-full w-full max-w-3xl mx-auto bg-background md:pt-8 pt-4 px-4 pb-8 relative shadow-sm overflow-hidden">
      {/* Header */}
      {step !== "success" && (
        <div className="flex items-center px-4 pb-4 border-b border-border">
          <button onClick={() => step === "search" ? navigate(-1) : setStep("search")} className="p-2 text-primary active:scale-95 transition-transform font-medium">
            Cancel
          </button>
          <h2 className="flex-1 text-center font-semibold text-lg">Request Money</h2>
          <div className="w-14" /> {/* Spacer */}
        </div>
      )}

      <div className="flex-1 overflow-y-auto px-4 py-6 relative">
        <AnimatePresence mode="wait">
          {step === "search" && (
            <motion.div key="search" initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} exit={{ opacity: 0, x: -20 }} className="space-y-6">
              <div className="relative">
                <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-muted-foreground" size={20} />
                <Input className="pl-12 bg-surface" placeholder="Phone number, name, or PVL ID" />
              </div>
              
              <div>
                <h3 className="text-sm font-semibold text-muted-foreground mb-4 uppercase tracking-wider">Request From</h3>
                <div className="space-y-3">
                  {users.map(contact => (
                    <button 
                      key={contact.id} 
                      onClick={() => handleSelectRecipient(contact)}
                      className="w-full flex items-center gap-4 p-3 rounded-2xl bg-surface border border-border hover:bg-accent active:scale-[0.98] transition-all"
                    >
                      <div className="w-12 h-12 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-lg">
                        {(contact.name || "User").charAt(0).toUpperCase()}
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
                  {(recipient.name || "User").charAt(0).toUpperCase()}
                </div>
                <h3 className="text-xl font-bold">{recipient.name}</h3>
                <p className="text-muted-foreground text-sm">PVL-{recipient.id.toString().padStart(8, '0')}</p>
              </div>

              <div className="flex-1 flex flex-col items-center justify-center mt-8">
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
                
                <div className="w-full mt-10">
                   <Input 
                      placeholder="Add a note (e.g. For dinner)" 
                      value={message}
                      onChange={(e) => setMessage(e.target.value)}
                      className="bg-surface border-border text-center"
                   />
                </div>
              </div>

              <Button 
                onClick={handleRequestSubmit} 
                disabled={!amount || Number(amount) <= 0 || isProcessing}
                className="w-full mt-auto mb-6"
                size="lg"
              >
                {isProcessing ? "Processing..." : `Request ₹${amount || 0}`}
                {!isProcessing && <ArrowRight size={18} className="ml-2" />}
              </Button>
            </motion.div>
          )}

          {step === "success" && (
            <motion.div key="success" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="flex flex-col items-center justify-center h-full text-center mt-20">
              <motion.div 
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ type: "spring", stiffness: 200, damping: 15 }}
                className="w-24 h-24 rounded-full bg-success/20 flex items-center justify-center mb-6"
              >
                <CheckCircle2 className="text-success w-12 h-12" />
              </motion.div>
              <h2 className="text-3xl font-bold mb-2">Request Sent!</h2>
              <p className="text-muted-foreground mb-8">You requested ₹{amount} from {recipient?.name}. They will be notified via their app.</p>
              
              <Button onClick={() => navigate("/")} variant="outline" className="w-full max-w-xs" size="lg">
                Back to Home
              </Button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </PageTransition>
  );
}
