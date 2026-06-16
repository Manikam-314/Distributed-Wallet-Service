import { useEffect, useState, useRef } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { motion, AnimatePresence } from "framer-motion";
import { PremiumWalletCard } from "@/components/wallet/PremiumWalletCard";
import { QuickActionGrid } from "@/components/wallet/QuickActionGrid";
import { PendingRequests } from "@/components/wallet/PendingRequests";
import { useAuthStore } from "@/store/authStore";
import { useWalletStore } from "@/store/walletStore";
import { walletAPI } from "@/services/walletAPI";
import { transactionAPI } from "@/services/transactionAPI";
import { moneyRequestAPI } from "@/services/moneyRequestAPI";
import { Bell, ArrowUpRight, ArrowDownLeft, Clock, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Transaction } from "@/types";
import { useNavigate } from "react-router-dom";
import { formatBackendDate } from "@/lib/utils";

export function Home() {
  const { user } = useAuthStore();
  const { wallet, setBalance, pendingRequests } = useWalletStore();
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showPendingModal, setShowPendingModal] = useState(false);
  const notifRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    if (wallet?.walletId) {
      const fetchData = () => {
        walletAPI.getBalance(wallet.walletId)
          .then(newBalance => setBalance(newBalance))
          .catch(console.error);
          
        transactionAPI.getHistory(wallet.walletId)
          .then(data => {
            const sorted = data.sort((a, b) => 
               new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
            );
            setRecentTransactions(sorted.slice(0, 3));
          })
          .catch(console.error);
          
        if (user?.id) {
          moneyRequestAPI.getPendingRequests(user.id)
            .then(data => useWalletStore.getState().setPendingRequests(data))
            .catch(console.error);
        }
      };

      fetchData();
      const interval = setInterval(fetchData, 10000); // Poll every 10 seconds
      
      return () => clearInterval(interval);
    }
  }, [wallet?.walletId, setBalance]);

  // Click outside to close notifications
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (notifRef.current && !notifRef.current.contains(event.target as Node)) {
        setShowNotifications(false);
      }
    }
    if (showNotifications) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [showNotifications]);

  return (
    <PageTransition className="flex flex-col min-h-full w-full max-w-3xl mx-auto bg-background pb-8 md:pt-4">
      {/* Header Profile Section */}
      <header className="flex justify-between items-center p-6 pb-2">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-lg border border-primary/20">
            {user?.name?.charAt(0) || "U"}
          </div>
          <div>
            <p className="text-xs text-muted-foreground font-medium">Good Morning,</p>
            <h1 className="text-lg font-bold text-foreground">
              {user ? user.name.split(' ')[0] : "User"}
            </h1>
          </div>
        </div>
        
        <div className="relative" ref={notifRef}>
          <button 
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative p-2 rounded-full bg-surface shadow-sm border border-border hover:bg-accent active:scale-95 transition-all"
          >
            <Bell size={20} className="text-foreground" />
            {pendingRequests && pendingRequests.length > 0 && (
              <span className="absolute top-2 right-2.5 w-2 h-2 bg-danger rounded-full border border-surface"></span>
            )}
          </button>

          <AnimatePresence>
            {showNotifications && (
              <motion.div
                initial={{ opacity: 0, y: 10, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, scale: 0.95, transition: { duration: 0.15 } }}
                className="absolute right-0 top-12 w-72 bg-background/80 backdrop-blur-xl border border-border shadow-2xl rounded-2xl overflow-hidden z-50 origin-top-right"
              >
                <div className="p-3 border-b border-border bg-surface/50 font-semibold text-sm">
                  Notifications
                </div>
                <div className="max-h-80 overflow-y-auto p-2">
                  {pendingRequests && pendingRequests.length > 0 ? (
                    pendingRequests.map(req => (
                      <div 
                        key={req.id} 
                        className="p-3 hover:bg-accent/50 rounded-xl cursor-pointer transition-colors border border-transparent hover:border-border/50"
                        onClick={() => {
                          setShowNotifications(false);
                          document.getElementById('pending-requests-section')?.scrollIntoView({ behavior: 'smooth' });
                        }}
                      >
                        <p className="text-sm font-medium text-foreground">
                          Money Request
                        </p>
                        <p className="text-xs text-muted-foreground mt-0.5">
                          You have a pending request for ₹{req.amount}
                        </p>
                        <p className="text-[10px] text-primary font-medium mt-1">Tap to review</p>
                      </div>
                    ))
                  ) : (
                    <div className="p-6 text-center text-muted-foreground text-sm">
                      No new notifications
                    </div>
                  )}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </header>

      <div className="px-5">
        <PremiumWalletCard />
        
        {/* Pending Requests Summary/Trigger */}
        {pendingRequests && pendingRequests.length > 0 && (
          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-4 p-4 bg-primary/10 border border-primary/20 rounded-2xl flex justify-between items-center"
          >
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center text-primary">
                <Clock size={20} />
              </div>
              <div>
                <p className="font-bold text-foreground">Pending Requests</p>
                <p className="text-xs text-muted-foreground">You have {pendingRequests.length} requests waiting</p>
              </div>
            </div>
            <Button size="sm" onClick={() => setShowPendingModal(true)}>View All</Button>
          </motion.div>
        )}
        
        {/* Glassmorphism Modal for Pending Requests */}
        <AnimatePresence>
          {showPendingModal && (
            <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowPendingModal(false)}
                className="absolute inset-0 bg-background/40 backdrop-blur-md"
              />
              <motion.div
                initial={{ opacity: 0, scale: 0.95, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95, y: 20 }}
                className="relative w-full max-w-lg max-h-[85vh] overflow-y-auto bg-surface/80 backdrop-blur-2xl border border-white/20 shadow-2xl rounded-3xl p-6"
              >
                <div className="flex justify-between items-center mb-6">
                   <div className="flex items-center gap-2">
                      <div className="w-2 h-2 rounded-full bg-primary animate-pulse" />
                      <h2 className="text-xl font-bold">Incoming Requests</h2>
                   </div>
                   <button 
                    onClick={() => setShowPendingModal(false)}
                    className="p-2 rounded-full hover:bg-muted transition-colors"
                   >
                     <XCircle size={24} className="text-muted-foreground" />
                   </button>
                </div>
                <PendingRequests />
              </motion.div>
            </div>
          )}
        </AnimatePresence>
        
        <QuickActionGrid />
        
        {/* Recent Transactions */}
        <section className="mt-2">
          <div className="flex justify-between items-end mb-4">
            <h3 className="text-lg font-bold">Recent Activity</h3>
            <button onClick={() => navigate("/history")} className="text-sm font-medium text-primary active:scale-95 transition-transform">See All</button>
          </div>
          
          <div className="space-y-3 pb-24">
            {recentTransactions.length > 0 ? (
              recentTransactions.map((tx: Transaction) => {
                const isSent = tx.senderWalletId === wallet?.walletId;
                const statusColor = tx.status === "SUCCESS" ? "text-emerald-500" : tx.status === "FAILED" ? "text-rose-500" : "text-amber-500";
                
                return (
                  <div key={tx.id} className="flex items-center gap-4 p-4 bg-surface border border-border rounded-2xl active:scale-[0.98] transition-all cursor-pointer">
                    <div className={`w-12 h-12 rounded-full flex items-center justify-center ${isSent ? "bg-rose-500/10 text-rose-500" : "bg-emerald-500/10 text-emerald-500"}`}>
                      {isSent ? <ArrowUpRight size={20} /> : <ArrowDownLeft size={20} />}
                    </div>
                    
                    <div className="flex-1">
                      <p className="font-semibold text-foreground text-sm">
                        {isSent ? `Transfer to PVL-${tx.receiverWalletId}` : `Transfer from PVL-${tx.senderWalletId}`}
                      </p>
                      <p className="text-xs text-muted-foreground mt-0.5">{formatBackendDate(tx.timestamp).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'})}</p>
                    </div>
                    
                    <div className="text-right">
                      <p className={`font-bold text-sm ${isSent ? 'text-foreground' : 'text-emerald-500'}`}>
                        {isSent ? "-" : "+"}₹{tx.amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                      </p>
                      <p className={`text-[10px] font-medium ${statusColor} uppercase mt-1 tracking-wider`}>{tx.status}</p>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="bg-surface rounded-2xl border border-border p-5 shadow-sm text-center">
                <p className="text-sm text-muted-foreground">Transactions will appear here</p>
              </div>
            )}
          </div>
        </section>
      </div>
    </PageTransition>
  );
}
