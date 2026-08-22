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
import { notificationAPI, type InAppNotification } from "@/services/notificationAPI";
import { Bell, ArrowUpRight, ArrowDownLeft, Clock, XCircle, Info } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { Transaction } from "@/types";
import { useNavigate } from "react-router-dom";
import { formatBackendDate } from "@/lib/utils";

function timeAgo(dateStr: string) {
  const seconds = Math.floor((Date.now() - new Date(dateStr).getTime()) / 1000);
  if (seconds < 60) return `${seconds}s`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h`;
  const days = Math.floor(hours / 24);
  return `${days}d`;
}

export function Home() {
  const { user, isBankLinked, linkedBankName, linkedAccountNumber, isUpiPinSet } = useAuthStore();
  const { wallet, setBalance, pendingRequests } = useWalletStore();
  const [recentTransactions, setRecentTransactions] = useState<Transaction[]>([]);
  const [notifications, setNotifications] = useState<InAppNotification[]>([]);
  const [showNotifications, setShowNotifications] = useState(false);
  const [showPendingModal, setShowPendingModal] = useState(false);
  
  // Contextual Setup States
  const [showSetupModal, setShowSetupModal] = useState(false);
  const [setupModalType, setSetupModalType] = useState<"bank" | "upi">("bank");
  const [showBalancePrompt, setShowBalancePrompt] = useState(false);

  const notifRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return "Good morning";
    if (hour < 17) return "Good afternoon";
    return "Good evening";
  };

  useEffect(() => {
    if (wallet?.walletId) {
      const fetchData = () => {
        walletAPI.getBalance(wallet.walletId)
          .then(newBalance => setBalance(newBalance))
          .catch(console.error);

        transactionAPI.getHistory(wallet.walletId)
          .then(data => {
            const sorted = data.sort((a, b) =>
              new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
            );
            setRecentTransactions(sorted.slice(0, 3));
          })
          .catch(console.error);

        if (user?.id) {
          moneyRequestAPI.getPendingRequests(user.id)
            .then(data => useWalletStore.getState().setPendingRequests(data))
            .catch(console.error);

          notificationAPI.getUserNotifications(user.email, user.mobileNumber)
            .then(data => setNotifications(data))
            .catch(console.error);
        }
      };

      fetchData();
      const interval = setInterval(fetchData, 10000); // Poll every 10 seconds

      return () => clearInterval(interval);
    }
  }, [wallet?.walletId, setBalance, user?.id, user?.email, user?.mobileNumber]);

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

  const handleAction = (actionId: string, path: string) => {
    // Actions requiring Bank and UPI setup
    const requiresSetup = ["scan", "pay", "to_mobile", "to_bank", "add_money", "check_balance"];
    
    if (requiresSetup.includes(actionId)) {
      if (!isBankLinked) {
        setSetupModalType("bank");
        setShowSetupModal(true);
        return;
      }
      if (!isUpiPinSet) {
        setSetupModalType("upi");
        setShowSetupModal(true);
        return;
      }
    }

    // If check balance is clicked and verified
    if (actionId === "check_balance") {
      setShowBalancePrompt(true);
      setTimeout(() => setShowBalancePrompt(false), 5000);
      return;
    }

    // Default navigation
    navigate(path);
  };

  const handleSetupRedirect = () => {
    setShowSetupModal(false);
    navigate("/bank-link");
  };

  const maskValue = (val?: string) => {
    if (!val) return "XXXX-XXXX";
    if (val.length <= 4) return val;
    return `XXXX-XXXX-${val.slice(-4)}`;
  };

  return (
    <PageTransition className="flex flex-col min-h-full w-full max-w-3xl mx-auto bg-background pb-8 md:pt-4">
      {/* Header Profile Section */}
      <header className="flex justify-between items-center p-6 pb-2 sticky top-0 z-30 bg-background/80 backdrop-blur-md">
        <div className="flex items-center gap-3">
          <div className="w-12 h-12 rounded-[18px] bg-gradient-to-br from-primary/20 to-primary/5 flex items-center justify-center text-primary font-bold text-xl border border-primary/20 shadow-inner">
            {user?.name?.charAt(0) || "U"}
          </div>
          <div>
            <p className="text-[10px] text-muted-foreground font-bold uppercase tracking-widest opacity-70">
              {getGreeting()},
            </p>
            <h1 className="text-xl font-black text-foreground tracking-tight">
              {user ? user.name.split(' ')[0] : "User"}
            </h1>
            <p className="text-[10px] text-muted-foreground font-semibold mt-0.5 opacity-60">
              Wallet ID: PVL-{wallet?.walletId || "XXXXXX"}
            </p>
          </div>
        </div>

        <div className="relative" ref={notifRef}>
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={() => setShowNotifications(!showNotifications)}
            className="p-2.5 rounded-2xl bg-surface/50 backdrop-blur-xl border border-border/50 shadow-[0_4px_12px_rgba(0,0,0,0.05)] hover:bg-surface transition-all"
          >
            <Bell size={22} className="text-foreground/80" />
            {pendingRequests && pendingRequests.length > 0 && (
              <span className="absolute top-2.5 right-2.5 w-2.5 h-2.5 bg-danger rounded-full border-2 border-background animate-pulse"></span>
            )}
          </motion.button>

          <AnimatePresence>
            {showNotifications && (
              <motion.div
                initial={{ opacity: 0, y: 15, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: 10, scale: 0.95 }}
                className="absolute right-0 mt-3 w-80 bg-surface/95 backdrop-blur-2xl border border-border/80 rounded-[24px] shadow-[0_20px_50px_rgba(0,0,0,0.15)] overflow-hidden z-50 origin-top-right"
              >
                <div className="p-5 border-b border-border/50 flex justify-between items-center bg-accent/20">
                  <h3 className="font-bold text-foreground tracking-tight">Notifications</h3>
                  {(pendingRequests.length > 0 || notifications.length > 0) && (
                    <span className="text-[10px] font-black bg-primary text-primary-foreground px-2 py-0.5 rounded-full uppercase">
                      {pendingRequests.length + notifications.length} New
                    </span>
                  )}
                </div>
                <div className="max-h-96 overflow-y-auto p-2 space-y-1">
                  {/* PENDING REQUESTS ACTION ITEMS */}
                  {pendingRequests.map(req => (
                    <div
                      key={`req-${req.id}`}
                      className="p-4 hover:bg-primary/5 rounded-2xl cursor-pointer transition-all border border-transparent hover:border-primary/20 group"
                      onClick={() => {
                        setShowNotifications(false);
                        setShowPendingModal(true);
                      }}
                    >
                      <div className="flex gap-3">
                        <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center text-primary group-hover:bg-primary group-hover:text-white transition-colors">
                          <ArrowDownLeft size={18} />
                        </div>
                        <div className="flex-1">
                          <p className="text-sm font-bold text-foreground">Money Request</p>
                          <p className="text-xs text-muted-foreground mt-0.5 line-clamp-1">
                            ₹{req.amount} from {req.requesterId}
                          </p>
                          <div className="flex items-center gap-2 mt-2">
                            <span className="text-[10px] font-extrabold text-primary uppercase tracking-tighter bg-primary/10 px-1.5 py-0.5 rounded">Action Required</span>
                            <span className="text-[10px] text-muted-foreground/60">{timeAgo(req.createdAt)} ago</span>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}

                  {/* IN-APP NOTIFICATIONS (Reminders, etc) */}
                  {notifications.map(notif => (
                    <div
                      key={`notif-${notif.id}`}
                      className={`p-4 hover:bg-accent/50 rounded-2xl cursor-pointer transition-all border border-transparent hover:border-border/50 ${notif.isRead ? 'opacity-60' : ''}`}
                      onClick={() => {
                        notificationAPI.markAsRead(notif.id);
                        setNotifications(prev => prev.map(n => n.id === notif.id ? { ...n, isRead: true } : n));
                      }}
                    >
                      <div className="flex gap-3">
                        <div className="w-10 h-10 rounded-full bg-accent flex items-center justify-center text-muted-foreground">
                          <Info size={18} />
                        </div>
                        <div className="flex-1">
                          <p className="text-sm font-medium text-foreground leading-snug">{notif.message}</p>
                          <p className="text-[10px] text-muted-foreground/60 mt-1">{timeAgo(notif.createdAt)} ago</p>
                        </div>
                        {!notif.isRead && <div className="w-2 h-2 rounded-full bg-primary mt-2"></div>}
                      </div>
                    </div>
                  ))}

                  {pendingRequests.length === 0 && notifications.length === 0 && (
                    <div className="py-12 px-6 text-center">
                      <div className="w-16 h-16 rounded-full bg-muted/50 flex items-center justify-center mx-auto mb-4 text-muted-foreground/40">
                        <Bell size={32} />
                      </div>
                      <p className="text-sm font-bold text-foreground/50">All caught up!</p>
                      <p className="text-xs text-muted-foreground/50">No new alerts or requests</p>
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

        {/* Dynamic Balance Toast notification */}
        <AnimatePresence>
          {showBalancePrompt && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="mt-3 p-4 bg-emerald-500/10 border border-emerald-500/20 text-emerald-500 rounded-2xl flex items-center justify-between shadow-md"
            >
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                <span className="font-semibold text-sm">Available Balance:</span>
              </div>
              <span className="font-extrabold text-base">₹{wallet?.balance.toLocaleString("en-IN", { minimumFractionDigits: 2 })}</span>
            </motion.div>
          )}
        </AnimatePresence>

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

        {/* Main Action Grid */}
        <QuickActionGrid onAction={handleAction} />

        {/* Contextual Setup Modal */}
        <AnimatePresence>
          {showSetupModal && (
            <div className="fixed inset-0 z-[110] flex items-center justify-center p-4">
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowSetupModal(false)}
                className="absolute inset-0 bg-background/60 backdrop-blur-md"
              />
              <motion.div
                initial={{ opacity: 0, scale: 0.9, y: 30 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.9, y: 30 }}
                className="relative w-full max-w-md bg-surface border border-border shadow-2xl rounded-3xl p-6 z-10 text-center"
              >
                <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center mx-auto mb-4 text-primary">
                  <Clock size={32} />
                </div>
                <h3 className="text-xl font-bold text-foreground mb-2">Complete UPI Setup to Continue</h3>
                <p className="text-sm text-muted-foreground mb-6">
                  {setupModalType === "bank" 
                    ? "Link your bank account to enable secure transfers, scanning, and payments directly from your bank."
                    : "Create your secure 6-digit UPI PIN to authorize transactions instantly."}
                </p>
                <div className="flex flex-col gap-3">
                  <Button size="lg" className="w-full" onClick={handleSetupRedirect}>
                    {setupModalType === "bank" ? "Link Bank Account" : "Set UPI PIN"}
                  </Button>
                  <Button variant="ghost" size="lg" className="w-full" onClick={() => setShowSetupModal(false)}>
                    Cancel
                  </Button>
                </div>
              </motion.div>
            </div>
          )}
        </AnimatePresence>

        {/* SECONDARY BLOCKS */}
        <section className="grid grid-cols-2 gap-4 mt-2 mb-6">
          {/* Bank Account Status */}
          <div className="p-4 bg-surface/30 border border-border/60 rounded-2xl flex flex-col justify-between min-h-[110px]">
            <div>
              <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">Bank Account</p>
              <h4 className="font-bold text-sm text-foreground mt-1.5">
                {isBankLinked ? linkedBankName : "Not Linked"}
              </h4>
              <p className="text-xs text-muted-foreground mt-0.5">
                {isBankLinked ? maskValue(linkedAccountNumber) : "Add bank account"}
              </p>
            </div>
            {!isBankLinked ? (
              <button onClick={() => handleAction("to_bank", "/bank-link")} className="text-xs text-primary font-bold text-left mt-2 active:opacity-75">
                Link Account +
              </button>
            ) : (
              <span className="text-[10px] text-emerald-500 font-bold mt-2 flex items-center gap-1">
                Active ✓
              </span>
            )}
          </div>

          {/* UPI Setup Status */}
          <div className="p-4 bg-surface/30 border border-border/60 rounded-2xl flex flex-col justify-between min-h-[110px]">
            <div>
              <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">UPI Security</p>
              <h4 className="font-bold text-sm text-foreground mt-1.5">
                {isUpiPinSet ? "UPI PIN Configured" : "UPI PIN Missing"}
              </h4>
              <p className="text-xs text-muted-foreground mt-0.5">
                {isUpiPinSet ? "Secured via 6-digit PIN" : "Setup security PIN"}
              </p>
            </div>
            {!isUpiPinSet ? (
              <button onClick={() => handleAction("set_upi", "/bank-link")} className="text-xs text-primary font-bold text-left mt-2 active:opacity-75">
                Configure PIN →
              </button>
            ) : (
              <span className="text-[10px] text-emerald-500 font-bold mt-2 flex items-center gap-1">
                Secured ✓
              </span>
            )}
          </div>
        </section>

        {/* Smart Reminders / Bills */}
        <section className="mb-6">
          <h3 className="text-md font-bold mb-3 tracking-tight">Smart Reminders</h3>
          <div className="space-y-3">
            <div className="p-4 bg-surface/20 border border-border/40 rounded-2xl flex justify-between items-center">
              <div>
                <p className="font-bold text-sm">BESCOM Electricity Bill</p>
                <p className="text-xs text-muted-foreground">Due in 5 days • Wallet Autopay enabled</p>
              </div>
              <div className="text-right">
                <p className="font-extrabold text-sm">₹1,420.00</p>
                <button className="text-[10px] text-primary font-black uppercase mt-1">Pay Now</button>
              </div>
            </div>
            <div className="p-4 bg-surface/20 border border-border/40 rounded-2xl flex justify-between items-center">
              <div>
                <p className="font-bold text-sm">ACT Fibernet Internet</p>
                <p className="text-xs text-muted-foreground">Due in 9 days</p>
              </div>
              <div className="text-right">
                <p className="font-extrabold text-sm">₹1,179.00</p>
                <button className="text-[10px] text-primary font-black uppercase mt-1">Pay Now</button>
              </div>
            </div>
          </div>
        </section>

        {/* Offers / Rewards */}
        <section className="mb-6">
          <h3 className="text-md font-bold mb-3 tracking-tight">Offers & Cashback</h3>
          <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-none">
            <div className="p-4 bg-gradient-to-br from-indigo-500/10 to-purple-500/10 border border-indigo-500/20 rounded-2xl min-w-[220px] max-w-[220px]">
              <span className="text-[9px] font-black uppercase bg-indigo-500 text-white px-2 py-0.5 rounded-full">New User</span>
              <p className="font-bold text-sm mt-3">Flat ₹50 Cashback</p>
              <p className="text-xs text-muted-foreground mt-1">On linking your first bank account today</p>
            </div>
            <div className="p-4 bg-gradient-to-br from-emerald-500/10 to-teal-500/10 border border-emerald-500/20 rounded-2xl min-w-[220px] max-w-[220px]">
              <span className="text-[9px] font-black uppercase bg-emerald-500 text-white px-2 py-0.5 rounded-full">Cashback</span>
              <p className="font-bold text-sm mt-3">Up to ₹500 Cashback</p>
              <p className="text-xs text-muted-foreground mt-1">On sending money via UPI for 3 consecutive days</p>
            </div>
          </div>
        </section>

        {/* Recent Transactions */}
        <section className="mt-2">
          <div className="flex justify-between items-end mb-4">
            <h3 className="text-md font-bold tracking-tight">Recent Activity</h3>
            <button onClick={() => navigate("/history")} className="text-sm font-medium text-primary active:scale-95 transition-transform">See All</button>
          </div>

          <div className="space-y-3 pb-24">
            {recentTransactions.length > 0 ? (
              recentTransactions.map((tx: Transaction) => {
                const isSent = tx.senderWalletId === wallet?.walletId;
                const statusColor = tx.status === "SUCCESS" ? "text-emerald-500" : tx.status === "FAILED" ? "text-rose-500" : "text-amber-500";

                return (
                  <motion.div
                    initial={{ opacity: 0, x: -10 }}
                    animate={{ opacity: 1, x: 0 }}
                    key={tx.id}
                    className="group flex items-center gap-4 p-4 bg-surface/40 backdrop-blur-sm border border-border/50 rounded-2xl hover:bg-surface/80 hover:border-primary/20 hover:shadow-[0_8px_20px_rgba(0,0,0,0.04)] transition-all cursor-pointer"
                  >
                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center transition-transform group-hover:scale-110 ${isSent ? "bg-rose-500/10 text-rose-500" : "bg-emerald-500/10 text-emerald-500"}`}>
                      {isSent ? <ArrowUpRight size={22} /> : <ArrowDownLeft size={22} />}
                    </div>

                    <div className="flex-1">
                      <p className="font-bold text-foreground text-[15px]">
                        {isSent ? `Transfer Out` : `Transfer In`}
                      </p>
                      <p className="text-[11px] text-muted-foreground font-medium flex items-center gap-1.5 opacity-70">
                        {isSent ? `to PVL-${tx.receiverWalletId}` : `from PVL-${tx.senderWalletId}`}
                        <span className="w-1 h-1 rounded-full bg-border" />
                        {formatBackendDate(tx.createdAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                      </p>
                    </div>

                    <div className="text-right">
                      <p className={`font-black text-[16px] ${isSent ? 'text-foreground' : 'text-emerald-500'}`}>
                        {isSent ? "-" : "+"}₹{tx.amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                      </p>
                      <div className="flex justify-end gap-1 mt-1">
                        <span className={`text-[9px] font-bold px-1.5 py-0.5 rounded-md ${tx.status === 'SUCCESS' ? 'bg-emerald-500/10' : 'bg-rose-500/10'} ${statusColor} uppercase tracking-tighter`}>{tx.status}</span>
                      </div>
                    </div>
                  </motion.div>
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
