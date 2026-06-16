import { PageTransition } from "@/components/animations/PageTransition";
import { CreditCard, PlusCircle, ShieldCheck, Snowflake } from "lucide-react";
import { Button } from "@/components/ui/button";

export function DigitalCards() {
  return (
    <PageTransition className="flex flex-col h-full w-full max-w-3xl mx-auto bg-background md:pt-12 pt-6 px-4 md:px-8 pb-8 overflow-y-auto">
      <div className="flex justify-between items-center mb-6 md:mb-8">
        <h1 className="text-2xl md:text-3xl font-bold">My Cards</h1>
        <button className="text-primary hover:text-primary/80 active:scale-95 transition-transform flex items-center gap-1 font-medium">
          <PlusCircle size={18} /> Add
        </button>
      </div>

      {/* Virtual Wallet Card */}
      <div className="relative w-full aspect-[1.58/1] rounded-2xl overflow-hidden shadow-xl mb-6 bg-gradient-to-br from-indigo-500 via-primary to-blue-800 p-6 flex flex-col justify-between text-white border border-white/20">
        <div className="absolute inset-0 bg-[url('https://www.transparenttextures.com/patterns/cubes.png')] opacity-20 mix-blend-overlay"></div>
        <div className="relative flex justify-between items-start">
          <div className="font-bold tracking-widest text-lg opacity-80">PVL Virtual</div>
          <ShieldCheck size={28} className="text-emerald-300" />
        </div>
        
        <div className="relative">
          <p className="font-mono text-xl tracking-[0.2em] mb-2 opacity-90">•••• •••• •••• 9241</p>
          <div className="flex justify-between items-end">
            <div>
              <p className="text-[10px] uppercase opacity-70 tracking-wider">Cardholder</p>
              <p className="font-medium">Manikam K</p>
            </div>
            <div className="text-right">
              <p className="text-[10px] uppercase opacity-70 tracking-wider">Expires</p>
              <p className="font-medium">12/28</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex gap-4 mb-8">
        <Button variant="outline" className="flex-1 flex flex-col items-center gap-2 h-auto py-4 rounded-2xl border-border">
          <Snowflake size={24} className="text-blue-500" />
          <span className="text-xs font-medium">Freeze Card</span>
        </Button>
        <Button variant="outline" className="flex-1 flex flex-col items-center gap-2 h-auto py-4 rounded-2xl border-border">
          <CreditCard size={24} className="text-primary" />
          <span className="text-xs font-medium">Card Details</span>
        </Button>
      </div>

      <h3 className="font-semibold text-lg mb-4">Linked Bank Accounts</h3>
      
      <div className="space-y-3">
        {/* Linked Bank Placeholder */}
        <div className="flex items-center gap-4 p-4 rounded-2xl bg-surface border border-border">
          <div className="w-12 h-12 rounded-lg bg-blue-50 flex items-center justify-center">
            <span className="font-bold text-blue-800 text-lg">H</span>
          </div>
          <div className="flex-1">
            <p className="font-semibold">HDFC Bank</p>
            <p className="text-sm text-muted-foreground">Savings •••• 4821</p>
          </div>
          <div className="bg-success/10 text-success px-2 py-1 rounded-full text-xs font-medium">
            Primary
          </div>
        </div>
      </div>
    </PageTransition>
  );
}
