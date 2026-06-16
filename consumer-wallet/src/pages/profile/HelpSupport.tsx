import { ProfileSectionLayout } from "@/components/layout/ProfileSectionLayout";
import { MessageCircle, FileText, PhoneCall, AlertTriangle, CheckCircle2 } from "lucide-react";
import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

export function HelpSupport() {
  const [toastMessage, setToastMessage] = useState("");

  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(""), 3000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage]);

  return (
    <ProfileSectionLayout title="Help & Support">
      <div className="flex flex-col items-center justify-center text-center mt-6 mb-10">
        <div className="w-20 h-20 bg-primary/10 text-primary rounded-[28px] flex items-center justify-center mb-6">
          <MessageCircle size={36} />
        </div>
        <h2 className="text-2xl font-bold mb-2">How can we help?</h2>
        <p className="text-muted-foreground">Get answers to your questions and resolve issues quickly.</p>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-8">
        <button 
          onClick={() => setToastMessage("Connecting to Chat Agent...")}
          className="bg-surface rounded-3xl p-6 border border-border shadow-sm flex flex-col items-center justify-center gap-3 active:scale-95 transition-all outline-none focus:ring-2 focus:ring-primary/20"
        >
          <div className="w-12 h-12 rounded-full bg-emerald-500/10 text-emerald-500 flex items-center justify-center">
            <MessageCircle size={24} />
          </div>
          <span className="font-semibold text-sm">Chat Support</span>
        </button>

        <button 
          onClick={() => setToastMessage("Calling +1-800-WALLET-01...")}
          className="bg-surface rounded-3xl p-6 border border-border shadow-sm flex flex-col items-center justify-center gap-3 active:scale-95 transition-all outline-none focus:ring-2 focus:ring-primary/20"
        >
          <div className="w-12 h-12 rounded-full bg-blue-500/10 text-blue-500 flex items-center justify-center">
            <PhoneCall size={24} />
          </div>
          <span className="font-semibold text-sm">Call Us</span>
        </button>
      </div>

      <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2 ml-2">Quick Links</h3>
      <div className="bg-surface rounded-3xl border border-border shadow-sm overflow-hidden mb-6">
        <button className="w-full text-left p-4 border-b border-border flex items-center justify-between hover:bg-accent/50 transition-colors">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-purple-500/10 text-purple-500 flex items-center justify-center">
              <FileText size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">FAQs</p>
              <p className="text-xs text-muted-foreground">Most common questions</p>
            </div>
          </div>
        </button>
        
        <button className="w-full text-left p-4 flex items-center justify-between hover:bg-accent/50 transition-colors">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-rose-500/10 text-rose-500 flex items-center justify-center">
              <AlertTriangle size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">Report an Issue</p>
              <p className="text-xs text-muted-foreground">Transactions, bugs or fraud</p>
            </div>
          </div>
        </button>
      </div>

      <div className="text-center mt-12 mb-4">
        <p className="text-xs text-muted-foreground font-medium">App Version 2.1.0-beta</p>
      </div>
      
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
    </ProfileSectionLayout>
  );
}
