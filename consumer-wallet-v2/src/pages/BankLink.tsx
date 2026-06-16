import { useState } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { Landmark, ArrowRight, CheckCircle2, Lock, ShieldCheck } from "lucide-react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";


export function BankLink() {
  const [step, setStep] = useState(1);
  const [bank, setBank] = useState("");
  const [acc, setAcc] = useState("");
  const [ifsc, setIfsc] = useState("");
  const [name, setName] = useState("");
  const [pin, setPinState] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);
  const navigate = useNavigate();

  const handleSearch = () => {
    setIsSearching(true);
    setTimeout(() => {
      setIsSearching(false);
      setStep(2);
    }, 1500);
  };

  const handleLink = () => {
    setIsVerifying(true);
    setTimeout(() => {
      setIsVerifying(false);
      setStep(3);
    }, 2000);
  };

  return (
    <PageTransition className="flex flex-col h-[100dvh] bg-background pt-10 px-5 pb-8 relative z-50">
      {step === 1 && (
        <div className="flex flex-col h-full animate-in fade-in slide-in-from-right-4">
          <h1 className="text-3xl font-bold mb-2">Link Bank Account</h1>
          <p className="text-muted-foreground mb-8">Add a bank to easily withdraw to and deposit from your real bank account.</p>
          
          <div className="space-y-4">
            <Input 
              placeholder="Search Bank Name (e.g., HDFC Bank)" 
              value={bank}
              onChange={(e) => setBank(e.target.value)}
              className="bg-surface"
            />
          </div>

          <Button 
            disabled={bank.length < 3 || isSearching} 
            size="lg" 
            className="w-full mt-auto" 
            onClick={handleSearch}
          >
            {isSearching ? "Searching Bank..." : <div className="flex items-center gap-2">Continue <ArrowRight size={18} /></div>}
          </Button>
        </div>
      )}

      {step === 2 && (
        <div className="flex flex-col h-full animate-in fade-in slide-in-from-right-4">
          <h1 className="text-2xl font-bold mb-6">Enter Account Details</h1>
          
          <div className="space-y-4 flex-1">
            <div className="bg-primary/5 border border-primary/20 p-4 rounded-2xl flex items-center gap-3 mb-6">
              <Landmark className="text-primary" />
              <p className="font-semibold">{bank}</p>
            </div>

            <Input 
              type="text" 
              placeholder="Account Number" 
              value={acc}
              onChange={(e) => setAcc(e.target.value)}
            />
            <Input 
              type="text" 
              placeholder="IFSC Code" 
              value={ifsc}
              onChange={(e) => setIfsc(e.target.value)}
            />
            <Input 
              type="text" 
              placeholder="Account Holder Name" 
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <Button 
            disabled={!acc || !ifsc || !name || isVerifying} 
            size="lg" 
            className="w-full mt-auto" 
            onClick={handleLink}
          >
            {isVerifying ? "Verifying with Bank..." : "Verify & Link Account"}
          </Button>
        </div>
      )}

      {step === 3 && (
        <div className="flex flex-col items-center justify-center h-full text-center animate-in zoom-in-95 duration-500">
          <div className="w-20 h-20 rounded-full bg-success/20 flex flex-col items-center justify-center mb-6 text-success">
            <CheckCircle2 size={40} />
          </div>
          <h2 className="text-3xl font-bold mb-3">Bank Linked!</h2>
          <p className="text-muted-foreground mb-8">Your {bank} account ending in {acc.slice(-4)} has been successfully verified.</p>
          
          <Button className="w-full max-w-xs" size="lg" onClick={() => setStep(4)}>
            Set UPI PIN
          </Button>
        </div>
      )}

      {step === 4 && (
        <div className="flex flex-col h-full animate-in fade-in slide-in-from-right-4 mt-8">
          <div className="w-16 h-16 bg-primary/10 rounded-[22px] flex items-center justify-center mb-6">
            <Lock className="text-primary" size={32} />
          </div>
          <h1 className="text-2xl font-bold mb-2">Create UPI PIN</h1>
          <p className="text-muted-foreground mb-8">Set a secure 6-digit PIN to authorize your payments.</p>
          
          <div className="space-y-4 flex-1 relative">
             <Input 
                type="password" 
                maxLength={6}
                placeholder="6-Digit PIN" 
                value={pin}
                onChange={(e) => setPinState(e.target.value.replace(/\D/g, ''))}
                className="text-center text-xl tracking-[1em]"
              />
          </div>

          <Button 
            disabled={pin.length !== 6} 
            size="lg" 
            className="w-full mt-auto" 
            onClick={() => {
              // API Call to save PIN on backend goes here
              setStep(5);
            }}
          >
            Secure Account
          </Button>
        </div>
      )}
      
      {step === 5 && (
        <div className="flex flex-col items-center justify-center h-full text-center animate-in zoom-in-95 duration-500">
          <div className="w-24 h-24 rounded-full bg-primary text-primary-foreground flex flex-col items-center justify-center mb-6 shadow-xl shadow-primary/20">
            <ShieldCheck size={48} />
          </div>
          <h2 className="text-3xl font-bold mb-3">All Set!</h2>
          <p className="text-muted-foreground mb-8 text-lg">Your account is fully verified and secured. Welcome to PayVault.</p>
          
          <Button className="w-full max-w-xs" size="lg" onClick={() => navigate("/")}>
            Go to Dashboard
          </Button>
        </div>
      )}
    </PageTransition>
  );
}
