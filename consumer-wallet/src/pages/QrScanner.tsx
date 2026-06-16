import { useEffect } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { QrCode, X } from "lucide-react";
import { useNavigate } from "react-router-dom";

export function QrScanner() {
  const navigate = useNavigate();

  // Simulate scanning a code successfully after 3 seconds
  useEffect(() => {
    const timer = setTimeout(() => {
      // Navigate to send money pre-filled or just back to send flow
      navigate("/pay/send");
    }, 3000);
    return () => clearTimeout(timer);
  }, [navigate]);

  return (
    <PageTransition className="flex flex-col h-[100dvh] bg-black text-white relative z-50">
      <div className="flex justify-between p-6">
        <button onClick={() => navigate(-1)} className="p-2 bg-white/20 rounded-full backdrop-blur-md">
          <X size={24} />
        </button>
      </div>

      <div className="flex-1 flex flex-col items-center justify-center px-8 relative">
        <h2 className="text-2xl font-bold mb-8 tracking-widest uppercase">Scan to Pay</h2>
        
        {/* Viewfinder simulation */}
        <div className="relative w-64 h-64 mb-8">
          <div className="absolute inset-0 border-2 border-primary/50 rounded-3xl overflow-hidden shadow-[0_0_0_9999px_rgba(0,0,0,0.8)]">
            {/* Scanning line animation */}
            <div className="w-full h-1 bg-primary blur-[2px] shadow-[0_0_10px_#2563EB] absolute top-0 animate-[scan_2s_ease-in-out_infinite]" />
          </div>
          {/* Corner markers */}
          <div className="absolute -top-1 -left-1 w-8 h-8 border-t-4 border-l-4 border-primary rounded-tl-3xl"></div>
          <div className="absolute -top-1 -right-1 w-8 h-8 border-t-4 border-r-4 border-primary rounded-tr-3xl"></div>
          <div className="absolute -bottom-1 -left-1 w-8 h-8 border-b-4 border-l-4 border-primary rounded-bl-3xl"></div>
          <div className="absolute -bottom-1 -right-1 w-8 h-8 border-b-4 border-r-4 border-primary rounded-br-3xl"></div>
          
          <div className="w-full h-full flex items-center justify-center opacity-20">
            <QrCode size={120} />
          </div>
        </div>

        <p className="text-center text-white/70 max-w-xs">Align the QR code within the frame to scan automatically.</p>
      </div>
    </PageTransition>
  );
}
