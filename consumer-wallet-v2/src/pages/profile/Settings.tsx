import { ProfileSectionLayout } from "@/components/layout/ProfileSectionLayout";
import { Bell, Moon, Globe, HelpCircle, CheckCircle2 } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

export function Settings() {
  const [notifications, setNotifications] = useState(true);
  const [emailAlerts, setEmailAlerts] = useState(false);
  const [toastMessage, setToastMessage] = useState("");
  const [darkMode, setDarkMode] = useState(false);

  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(""), 3000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage]);

  useEffect(() => {
    setDarkMode(document.documentElement.classList.contains("dark"));
  }, []);

  const toggleDarkMode = (checked: boolean) => {
    setDarkMode(checked);
    if (checked) {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  };

  return (
    <ProfileSectionLayout title="Settings">
      <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2 ml-2 mt-4">Preferences</h3>
      <div className="bg-surface rounded-3xl border border-border shadow-sm overflow-hidden mb-8">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-indigo-500/10 text-indigo-500 flex items-center justify-center">
              <Moon size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">Dark Mode</p>
              <p className="text-xs text-muted-foreground">Toggle application theme</p>
            </div>
          </div>
          <Switch checked={darkMode} onCheckedChange={toggleDarkMode} />
        </div>

        <button
          onClick={() => setToastMessage("Language preferences coming soon!")}
          className="w-full text-left p-4 flex items-center justify-between hover:bg-accent/50 transition-colors"
        >
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-cyan-500/10 text-cyan-500 flex items-center justify-center">
              <Globe size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">Language</p>
              <p className="text-xs text-muted-foreground">English (US)</p>
            </div>
          </div>
        </button>
      </div>

      <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wider mb-2 ml-2">Notifications</h3>
      <div className="bg-surface rounded-3xl border border-border shadow-sm overflow-hidden">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-blue-500/10 text-blue-500 flex items-center justify-center">
              <Bell size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">Push Notifications</p>
              <p className="text-xs text-muted-foreground w-48">Alerts for transfers & requests</p>
            </div>
          </div>
          <Switch
            checked={notifications}
            onCheckedChange={(c: boolean) => {
              setNotifications(c);
              setToastMessage(`Push notifications ${c ? 'enabled' : 'disabled'}`);
            }}
          />
        </div>

        <div className="p-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-amber-500/10 text-amber-500 flex items-center justify-center">
              <HelpCircle size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">Email Updates</p>
              <p className="text-xs text-muted-foreground w-48">Weekly summaries and promos</p>
            </div>
          </div>
          <Switch
            checked={emailAlerts}
            onCheckedChange={(c: boolean) => {
              setEmailAlerts(c);
              setToastMessage(`Email updates ${c ? 'enabled' : 'disabled'}`);
            }} 
          />
        </div>
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
