import { ProfileSectionLayout } from "@/components/layout/ProfileSectionLayout";
import { Fingerprint, EyeOff, LockKeyhole, CheckCircle2 } from "lucide-react";
import { Switch } from "@/components/ui/switch";
import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";

import { useSettingsStore } from "@/store/settingsStore";
import { useAuthStore } from "@/store/authStore";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { X } from "lucide-react";

export function SecurityPrivacy() {
  const { 
    biometrics, setBiometrics, 
    hideBalance, setHideBalance, 
    twoFactor, setTwoFactor 
  } = useSettingsStore();

  const { pin: _pin, setPin } = useAuthStore();
  const [toastMessage, setToastMessage] = useState("");
  const [showPinModal, setShowPinModal] = useState(false);
  const [newPin, setNewPin] = useState("");
  const [confirmPin, setConfirmPin] = useState("");
  const [pinError, setPinError] = useState("");

  useEffect(() => {
    if (toastMessage) {
      const timer = setTimeout(() => setToastMessage(""), 3000);
      return () => clearTimeout(timer);
    }
  }, [toastMessage]);

  const handlePinChange = () => {
    if (newPin.length !== 6) {
      setPinError("PIN must be 6 digits");
      return;
    }
    if (newPin !== confirmPin) {
      setPinError("PINs do not match");
      return;
    }
    setPin(newPin);
    setShowPinModal(false);
    setToastMessage("UPI PIN updated successfully!");
    setNewPin("");
    setConfirmPin("");
    setPinError("");
  };


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
        onClick={() => setShowPinModal(true)}
        className="w-full flex items-center gap-4 p-4 rounded-2xl bg-surface border border-border shadow-sm active:scale-[0.98] transition-all text-left"
      >
        <div className="w-10 h-10 rounded-full bg-rose-500/10 text-rose-500 flex items-center justify-center">
          <LockKeyhole size={20} />
        </div>
        <div className="flex-1">
          <p className="font-semibold text-foreground">Change UPI PIN</p>
          <p className="text-xs text-muted-foreground">Reset your 6-digit transaction PIN</p>
        </div>
      </button>

      <AnimatePresence>
        {showPinModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-background/80 backdrop-blur-sm">
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-surface border border-border p-6 rounded-3xl w-full max-w-sm shadow-2xl relative"
            >
              <button 
                onClick={() => setShowPinModal(false)}
                className="absolute top-4 right-4 p-2 rounded-full hover:bg-muted"
              >
                <X size={20} />
              </button>

              <h2 className="text-xl font-bold mb-1">Set New PIN</h2>
              <p className="text-sm text-muted-foreground mb-6">Enter a new 6-digit transaction PIN</p>

              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="text-xs font-bold uppercase text-muted-foreground ml-1">New PIN</label>
                  <Input 
                    type="password" 
                    maxLength={6} 
                    placeholder="Enter 6-digit PIN"
                    value={newPin}
                    onChange={(e) => {
                      setNewPin(e.target.value.replace(/\D/g, ''));
                      setPinError("");
                    }}
                    className="bg-muted/30 text-center tracking-[0.5em] text-lg py-6"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-xs font-bold uppercase text-muted-foreground ml-1">Confirm PIN</label>
                  <Input 
                    type="password" 
                    maxLength={6} 
                    placeholder="Repeat PIN"
                    value={confirmPin}
                    onChange={(e) => {
                      setConfirmPin(e.target.value.replace(/\D/g, ''));
                      setPinError("");
                    }}
                    className="bg-muted/30 text-center tracking-[0.5em] text-lg py-6"
                  />
                </div>

                {pinError && <p className="text-danger text-xs font-medium ml-1">{pinError}</p>}

                <Button 
                  onClick={handlePinChange}
                  className="w-full py-6 text-lg rounded-2xl mt-4"
                  disabled={newPin.length !== 6 || confirmPin.length !== 6}
                >
                  Update PIN
                </Button>
              </div>
            </motion.div>
          </div>
        )}

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
