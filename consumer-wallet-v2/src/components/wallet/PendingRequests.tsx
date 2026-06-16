import { useEffect, useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { moneyRequestAPI } from "@/services/moneyRequestAPI";
import type { MoneyRequest } from "@/services/moneyRequestAPI";
import { useWalletStore } from "@/store/walletStore";
import { authAPI } from "@/services/authAPI";
import { walletAPI } from "@/services/walletAPI";
import { CheckCircle, XCircle, Clock } from "lucide-react";
import { formatBackendDate } from "@/lib/utils";
import { Button } from "@/components/ui/button";

import { Input } from "@/components/ui/input";

export function PendingRequests() {
  const { wallet, balance, setBalance, pendingRequests, setPendingRequests, removeRequest } = useWalletStore();
  const [usersDict, setUsersDict] = useState<Record<number, string>>({});
  const [isProcessing, setIsProcessing] = useState<number | null>(null);
  const [payingRequestId, setPayingRequestId] = useState<number | null>(null);
  const [enteredPin, setEnteredPin] = useState("");
  const [pinError, setPinError] = useState("");

  useEffect(() => {
    if (wallet?.walletId) {
      const fetchRequests = () => {
        moneyRequestAPI.getPendingRequests(wallet.walletId) // Backend recipientId stores walletId
          .then(setPendingRequests)
          .catch(console.error);
      };

      fetchRequests();
      const interval = setInterval(fetchRequests, 15000); // Poll every 15 seconds
      
      // Build walletId → userName mapping
      authAPI.getUsers().then(async users => {
          const dict: Record<number, string> = {};
          for (const u of users) {
              try {
                  const w = await walletAPI.getWalletByUserId(u.id);
                  dict[w.walletId] = u.name;
              } catch (e) {
                  // If user has no wallet, skip
              }
          }
          setUsersDict(dict);
      }).catch(console.error);

      return () => clearInterval(interval);
    }
  }, [wallet?.walletId, wallet?.userId, setPendingRequests]);

  const handlePay = async (request: MoneyRequest) => {
    if (!wallet) return;
    if (balance < request.amount) {
        alert("Insufficient balance to pay this request.");
        return;
    }
    
    setIsProcessing(request.id);
    try {
      await moneyRequestAPI.payRequest(request.id, wallet.walletId);
      setBalance(balance - request.amount);
      removeRequest(request.id);
    } catch (e: any) {
      console.error(e);
      alert("Failed to pay request");
    } finally {
      setIsProcessing(null);
      setPayingRequestId(null);
      setEnteredPin("");
      setPinError("");
    }
  };

  const handleDecline = async (request: MoneyRequest) => {
    if (!wallet) return;
    setIsProcessing(request.id);
    try {
      await moneyRequestAPI.declineRequest(request.id, wallet.walletId);
      removeRequest(request.id);
    } catch (e: any) {
      console.error(e);
      alert("Failed to decline request");
    } finally {
      setIsProcessing(null);
    }
  };

  if (!pendingRequests || pendingRequests.length === 0) {
    return null; // Don't show anything if there are no requests
  }

  return (
    <section id="pending-requests-section" className="mt-2 mb-2 scroll-m-20">
      {/* Header removed for modal usage */}
      
      <div className="space-y-3">
        <AnimatePresence>
          {pendingRequests.map(req => {
              const requesterName = usersDict[req.requesterId] || `User ${req.requesterId}`;
              
              return (
                  <motion.div 
                    key={req.id}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95 }}
                    className="p-4 bg-surface border border-amber-200 shadow-sm rounded-2xl relative overflow-hidden"
                  >
                     {/* Soft background glow */}
                     <div className="absolute top-0 right-0 w-24 h-24 bg-amber-500/5 rounded-full blur-2xl -mr-10 -mt-10 pointer-events-none" />
                     
                     <div className="flex justify-between items-start mb-3">
                        <div>
                            <p className="font-semibold text-foreground text-sm flex items-center gap-1.5">
                                {requesterName} <span className="text-muted-foreground font-normal">is requesting</span>
                            </p>
                            <p className="text-xs text-muted-foreground mt-0.5">{formatBackendDate(req.createdAt).toLocaleString(undefined, { month: 'short', day: 'numeric'})}</p>
                        </div>
                        <p className="font-bold text-lg text-foreground">
                            ₹{req.amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                        </p>
                     </div>
                     
                     {req.message && (
                        <div className="bg-muted/50 p-2.5 rounded-xl mb-3 text-sm text-muted-foreground italic border border-border/50">
                           "{req.message}"
                        </div>
                     )}

                     {(req.extractedDueDate || req.extractedIntent) && (
                        <div className="bg-primary/5 border border-primary/20 p-2.5 rounded-xl mb-4 flex items-center gap-2">
                           <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center text-primary">
                             <Clock size={16} />
                           </div>
                           <div>
                              <p className="text-[10px] uppercase tracking-wider font-bold text-primary/60">Smart Reminder</p>
                              <p className="text-xs font-semibold text-foreground">
                                 {req.extractedIntent === 'repay' ? 'Repayment due' : 'Payment requested'} 
                                 {req.extractedDueDate ? ` by ${new Date(req.extractedDueDate).toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })}` : ''}
                              </p>
                           </div>
                        </div>
                     )}
                     
                     <div className="flex gap-2 w-full mt-2">
                       {payingRequestId === req.id ? (
                          <div className="w-full space-y-3 mt-1 bg-background/50 p-3 rounded-xl border border-border">
                             <p className="text-sm font-medium text-center">Enter 6-digit PIN</p>
                             <Input 
                                type="password" 
                                maxLength={6}
                                placeholder="******" 
                                autoFocus
                                value={enteredPin}
                                onChange={(e) => {
                                  setEnteredPin(e.target.value.replace(/\D/g, ''));
                                  setPinError("");
                                }}
                                className={`text-center tracking-[0.5em] bg-surface ${pinError ? "border-danger focus-visible:ring-danger" : ""}`}
                              />
                              {pinError && <p className="text-danger text-xs text-center font-medium mt-1">{pinError}</p>}
                              <div className="flex gap-2 pt-1">
                                <Button 
                                  variant="outline" 
                                  size="sm" 
                                  className="flex-1"
                                  disabled={isProcessing === req.id}
                                  onClick={() => {
                                      setPayingRequestId(null);
                                      setEnteredPin("");
                                      setPinError("");
                                  }}
                                >
                                  Cancel
                                </Button>
                                <Button 
                                  size="sm" 
                                  className="flex-1"
                                  disabled={enteredPin.length !== 6 || isProcessing === req.id}
                                  onClick={() => {
                                      handlePay(req); // Backend validates PIN strictly
                                  }}
                                >
                                  {isProcessing === req.id ? "Processing..." : "Confirm"}
                                </Button>
                              </div>
                          </div>
                       ) : (
                         <>
                         <Button 
                            variant="outline" 
                            className="flex-1 text-danger hover:text-danger hover:bg-danger/10 border-danger/20"
                            size="sm"
                            disabled={isProcessing === req.id}
                            onClick={() => handleDecline(req)}
                         >
                            <XCircle size={16} className="mr-1.5" /> Decline
                         </Button>
                         <Button 
                            className="flex-1"
                            size="sm"
                            disabled={isProcessing === req.id}
                            onClick={() => {
                                setPayingRequestId(req.id);
                                setEnteredPin("");
                                setPinError("");
                            }}
                         >
                            <CheckCircle size={16} className="mr-1.5" /> Pay Now
                         </Button>
                         </>
                       )}
                     </div>
                  </motion.div>
              )
          })}
        </AnimatePresence>
      </div>
    </section>
  );
}
