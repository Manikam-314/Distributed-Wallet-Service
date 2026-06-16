import { useState } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { CheckCircle2, ShieldCheck, UserCheck, Smartphone, Landmark, ScanFace, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/authStore";
import { useNavigate } from "react-router-dom";

const VERIFICATION_STEPS = [
  { id: 1, label: "Mobile verified", icon: Smartphone, status: "completed" },
  { id: 2, label: "Identity verified", icon: UserCheck, status: "completed" },
  { id: 3, label: "PAN verified", icon: ShieldCheck, status: "current" },
  { id: 4, label: "Aadhaar verified", icon: Landmark, status: "pending" },
  { id: 5, label: "Selfie verified", icon: ScanFace, status: "pending" },
];

export function KYC() {
  const { updateKyc } = useAuthStore();
  const [isProcessing, setIsProcessing] = useState(false);
  const [success, setSuccess] = useState(false);
  const navigate = useNavigate();

  const simulateVerification = () => {
    setIsProcessing(true);
    setTimeout(() => {
      setIsProcessing(false);
      setSuccess(true);
      updateKyc("VERIFIED");
    }, 2000);
  };

  return (
    <PageTransition className="flex flex-col h-[100dvh] bg-background pt-8 px-5 pb-8 relative z-50">
      <div className="flex-1 flex flex-col pt-12">
        <h1 className="text-3xl font-bold mb-2">Complete Full KYC</h1>
        <p className="text-muted-foreground mb-10">Verify your identity to unlock higher transaction limits and premium features.</p>
        
        <div className="space-y-6 flex-1">
          <div className="relative">
            {/* Connecting line */}
            <div className="absolute left-6 top-6 bottom-6 w-0.5 bg-border -z-10" />
            
            <div className="space-y-8">
              {VERIFICATION_STEPS.map((step) => {
                const isCompleted = step.status === "completed";
                const isCurrent = step.status === "current";
                return (
                  <div key={step.id} className={`flex items-start gap-4 ${isCompleted ? 'opacity-100' : isCurrent ? 'opacity-100' : 'opacity-40'}`}>
                    <div className={`w-12 h-12 rounded-full border-2 flex items-center justify-center bg-background
                      ${isCompleted ? 'border-primary text-primary' : isCurrent ? 'border-primary border-dashed text-primary' : 'border-border text-muted-foreground'}
                    `}>
                      {isCompleted ? <CheckCircle2 size={20} /> : <step.icon size={20} />}
                    </div>
                    <div className="flex-1 pt-3 border-b border-border pb-6 flex justify-between items-center">
                      <p className="font-semibold text-foreground">{step.label}</p>
                      {isCurrent && <ChevronRight size={18} className="text-muted-foreground" />}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
      
      {success ? (
        <div className="flex flex-col gap-4 animate-in fade-in slide-in-from-bottom-4">
          <div className="bg-success/10 border border-success/30 p-4 rounded-2xl flex items-center gap-3">
            <CheckCircle2 className="text-success" />
            <p className="text-sm font-medium text-success-foreground">You are fully verified!</p>
          </div>
          <Button size="lg" className="w-full" onClick={() => navigate("/bank-link")}>
            Set Up UPI & Bank
          </Button>
        </div>
      ) : (
        <Button 
          size="lg" 
          className="w-full mt-auto" 
          onClick={simulateVerification}
          disabled={isProcessing}
        >
          {isProcessing ? "Verifying digitally..." : "Continue Verification"}
        </Button>
      )}
    </PageTransition>
  );
}
