import { useState, useEffect } from "react";
import { PageTransition } from "@/components/animations/PageTransition";
import { Search, Filter, ArrowUpRight, ArrowDownLeft } from "lucide-react";
import { Input } from "@/components/ui/input";
import { transactionAPI } from "@/services/transactionAPI";
import { useWalletStore } from "@/store/walletStore";
import type { Transaction } from "@/types";
import { formatBackendDate } from "@/lib/utils";

export function TransactionHistory() {
  const [filter, setFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");
  const { wallet } = useWalletStore();
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (wallet?.walletId) {
      setIsLoading(true);
      transactionAPI.getHistory(wallet.walletId)
        .then(data => setTransactions(data))
        .catch(console.error)
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, [wallet?.walletId]);

  const filteredTransactions = transactions.filter(t => {
    // Determine type relative to current wallet
    const type = t.senderWalletId === wallet?.walletId ? "sent" : "received";
    
    if (filter !== "all" && type !== filter && t.status.toLowerCase() !== filter) return false;
    
    if (searchQuery) {
      const q = searchQuery.toLowerCase();
      // Searching by ID or amount
      return String(t.id).includes(q) || String(t.amount).includes(q);
    }
    
    return true;
  });

  return (
    <PageTransition className="flex flex-col h-full w-full max-w-3xl mx-auto bg-background md:pt-12 pt-6 px-4 md:px-8">
      <h1 className="text-2xl md:text-3xl font-bold mb-6 md:mb-8">History</h1>

      <div className="flex gap-2 mb-6">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
          <Input 
            className="pl-10 h-10 bg-surface rounded-xl" 
            placeholder="Search transactions..." 
            value={searchQuery}
            onChange={e => setSearchQuery(e.target.value)}
          />
        </div>
        <button className="h-10 w-10 bg-surface border border-border rounded-xl flex items-center justify-center text-foreground hover:bg-accent active:scale-95 transition-all">
          <Filter size={18} />
        </button>
      </div>

      <div className="flex gap-2 mb-6 overflow-x-auto pb-2 scrollbar-hide">
        {["All", "Sent", "Received", "Failed"].map((tab) => (
          <button
            key={tab}
            onClick={() => setFilter(tab.toLowerCase())}
            className={`px-4 py-1.5 rounded-full text-sm font-medium whitespace-nowrap transition-colors ${
              filter === tab.toLowerCase() ? "bg-primary text-white" : "bg-surface border border-border text-foreground hover:bg-accent"
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      <div className="flex-1 overflow-y-auto space-y-4 pb-12">
        {isLoading ? (
          <div className="flex justify-center p-8">
             <div className="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin"></div>
          </div>
        ) : filteredTransactions.length > 0 ? (
          filteredTransactions.map((tx: Transaction) => {
            const isSent = tx.senderWalletId === wallet?.walletId;
            const statusColor = tx.status === "SUCCESS" ? "text-emerald-500" : tx.status === "FAILED" ? "text-rose-500" : "text-amber-500";
            
            return (
              <div key={tx.id} className="flex items-center gap-4 p-4 bg-surface border border-border rounded-2xl active:scale-[0.98] transition-all cursor-pointer">
                <div className={`w-12 h-12 rounded-full flex items-center justify-center ${isSent ? "bg-rose-500/10 text-rose-500" : "bg-emerald-500/10 text-emerald-500"}`}>
                  {isSent ? <ArrowUpRight size={20} /> : <ArrowDownLeft size={20} />}
                </div>
                
                <div className="flex-1">
                  <p className="font-semibold text-foreground">
                    {isSent ? `Transfer to #${tx.receiverWalletId}` : `Transfer from #${tx.senderWalletId}`}
                  </p>
                  <p className="text-sm text-muted-foreground">{formatBackendDate(tx.createdAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'})}</p>
                </div>
                
                <div className="text-right">
                  <p className={`font-bold ${isSent ? 'text-foreground' : 'text-emerald-500'}`}>
                    {isSent ? "-" : "+"}₹{tx.amount.toLocaleString("en-IN", { minimumFractionDigits: 2 })}
                  </p>
                  <p className={`text-xs font-medium ${statusColor} uppercase mt-0.5`}>{tx.status}</p>
                </div>
              </div>
            );
          })
        ) : (
          <div className="text-center p-8 bg-surface rounded-2xl border border-border pb-12">
             <div className="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4 text-primary">
                <Filter size={24} />
             </div>
             <p className="font-medium text-lg">No transactions found</p>
             <p className="text-muted-foreground text-sm">Try adjusting your filters</p>
          </div>
        )}
      </div>
    </PageTransition>
  );
}
