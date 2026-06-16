import { ProfileSectionLayout } from "@/components/layout/ProfileSectionLayout";
import { ShieldAlert, Fingerprint, EyeOff, LockKeyhole, CheckCircle2 } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

export function SecurityPrivacy() {
  const [biometrics, setBiometrics] = useState(true);
  const [hideBalance, setHideBalance] = useState(false);
  const [twoFactor, setTwoFactor] = useState(false);
  const [toastMessage, setToastMessage] = useState("");

  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(""), 3000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage]);

  return (
    <ProfileSectionLayout title="Security & Privacy">
      <div className="bg-surface rounded-3xl border border-border shadow-sm overflow-hidden mb-6">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-indigo-500/10 text-indigo-500 flex items-center justify-center">
              <Fingerprint size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">Biometric Login</p>
              <p className="text-xs text-muted-foreground w-48">Use Face ID or Fingerprint to unlock</p>
            </div>
          </div>
          <Switch 
            checked={biometrics} 
            onCheckedChange={(c: boolean) => {
              setBiometrics(c);
              setToastMessage(`Biometric login ${c ? 'enabled' : 'disabled'}`);
            }} 
          />
        </div>

        <div className="p-4 border-b border-border flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-slate-500/10 text-slate-500 flex items-center justify-center">
              <EyeOff size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">Hide Balances</p>
              <p className="text-xs text-muted-foreground w-48">Obscure balance on Home Screen</p>
            </div>
          </div>
          <Switch 
            checked={hideBalance} 
            onCheckedChange={(c: boolean) => {
              setHideBalance(c);
              setToastMessage(`Balance hiding ${c ? 'enabled' : 'disabled'}`);
            }} 
          />
        </div>

        <div className="p-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-10 h-10 rounded-full bg-amber-500/10 text-amber-500 flex items-center justify-center">
              <LockKeyhole size={20} />
            </div>
            <div>
              <p className="font-semibold text-foreground">Two-Factor Auth</p>
              <p className="text-xs text-muted-foreground w-48">Require OTP for big transfers</p>
            </div>
          </div>
          <Switch 
            checked={twoFactor} 
            onCheckedChange={(c: boolean) => {
              setTwoFactor(c);
              setToastMessage(`Two-Factor Auth ${c ? 'enabled' : 'disabled'}`);
            }} 
          />
        </div>
      </div>

      <button 
        onClick={() => setToastMessage("Check email for PIN reset instructions.")}
        className="w-full flex items-center gap-4 p-4 rounded-2xl bg-surface border border-border shadow-sm active:scale-[0.98] transition-all text-left"
      >
        <div className="w-10 h-10 rounded-full bg-rose-500/10 text-rose-500 flex items-center justify-center">
          <ShieldAlert size={20} />
        </div>
        <div className="flex-1">
          <p className="font-semibold text-foreground">Change UPI PIN</p>
          <p className="text-xs text-muted-foreground">Reset your 6-digit transaction PIN</p>
        </div>
      </button>

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
