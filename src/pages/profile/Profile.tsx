import { useState, useEffect } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { useAuthStore } from "@/store/authStore";
import { Shield, User as UserIcon, Settings, HelpCircle, LogOut, ChevronRight, CheckCircle2 } from "lucide-react";
import { motion, AnimatePresence } from "framer-motion";
import { useNavigate } from "react-router-dom";

export function Profile() {
  const { user, logout } = useAuthStore();
  const [toastMessage, setToastMessage] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(""), 3000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage]);

  const MENU_ITEMS = [
    { icon: UserIcon, label: "Personal Details", color: "text-blue-500", path: "/profile/personal" },
    { icon: Shield, label: "Security & Privacy", color: "text-emerald-500", path: "/profile/security" },
    { icon: Settings, label: "Settings", color: "text-slate-500", path: "/profile/settings" },
    { icon: HelpCircle, label: "Help & Support", color: "text-purple-500", path: "/profile/support" },
  ];

  return (
    <PageTransition className="flex flex-col min-h-full w-full max-w-3xl mx-auto bg-background md:pt-12 pt-6 px-4 md:px-8 pb-8">
      <h1 className="text-2xl md:text-3xl font-bold mb-6 md:mb-8">Profile</h1>

      <div className="flex items-center gap-4 p-4 bg-surface rounded-2xl border border-border shadow-sm mb-6">
        <div className="w-16 h-16 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-2xl border border-primary/20">
          {user?.name?.charAt(0) || "U"}
        </div>
        <div className="flex-1">
          <h2 className="text-xl font-bold">{user?.name || "User Name"}</h2>
          <p className="text-sm text-muted-foreground">{user?.email || "user@example.com"}</p>
        </div>
      </div>

      <div className="bg-surface rounded-2xl border border-border shadow-sm mb-6 overflow-hidden">
        <div className="p-4 border-b border-border flex justify-between items-center bg-accent/30">
          <div>
            <h3 className="font-semibold text-sm uppercase tracking-wider text-muted-foreground mb-1">KYC Status</h3>
            <div className="flex items-center gap-1 font-medium text-success">
              <CheckCircle2 size={16} /> Fully Verified
            </div>
          </div>
          <button onClick={() => setToastMessage("Verification details are up to date.")} className="text-sm font-medium text-primary active:scale-95 transition-transform">View Details</button>
        </div>
      </div>

      <div className="bg-surface rounded-2xl border border-border shadow-sm mb-8 overflow-hidden">
        {MENU_ITEMS.map((item, index) => (
          <button 
            key={item.label}
            onClick={() => navigate(item.path)}
            className={`w-full flex items-center gap-4 p-4 active:bg-accent transition-colors ${
              index !== MENU_ITEMS.length - 1 ? "border-b border-border" : ""
            }`}
          >
            <div className={`w-10 h-10 rounded-full bg-accent flex items-center justify-center ${item.color}`}>
              <item.icon size={20} />
            </div>
            <span className="flex-1 text-left font-medium">{item.label}</span>
            <ChevronRight size={20} className="text-muted-foreground" />
          </button>
        ))}
      </div>

      <button 
        onClick={logout}
        className="w-full flex items-center justify-center gap-2 p-4 rounded-2xl bg-danger/10 text-danger font-medium hover:bg-danger/20 active:scale-[0.98] transition-all mt-auto"
      >
        <LogOut size={20} />
        Log Out
      </button>

      {/* Custom Toast Notification */}
      <AnimatePresence>
        {toastMessage && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.9 }}
            className="fixed bottom-24 left-1/2 -translate-x-1/2 bg-foreground text-background px-6 py-3 rounded-full shadow-lg font-medium text-sm flex items-center gap-2 z-50 whitespace-nowrap"
          >
            <CheckCircle2 size={16} className="text-primary" />
            {toastMessage}
          </motion.div>
        )}
      </AnimatePresence>
    </PageTransition>
  );
}
