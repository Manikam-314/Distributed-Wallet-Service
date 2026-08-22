import { motion } from "framer-motion";
import { Send, ArrowDownToLine, QrCode, PlusCircle, FileText, Smartphone, Landmark, Eye } from "lucide-react";

const ACTIONS = [
  { id: "scan", label: "Scan QR", icon: QrCode, path: "/pay/scan", color: "bg-purple-100 text-purple-600 dark:bg-purple-950/40 dark:text-purple-400" },
  { id: "pay", label: "Pay", icon: Send, path: "/pay/send", color: "bg-blue-100 text-blue-600 dark:bg-blue-950/40 dark:text-blue-400" },
  { id: "to_mobile", label: "To Mobile", icon: Smartphone, path: "/pay/send", color: "bg-cyan-100 text-cyan-600 dark:bg-cyan-950/40 dark:text-cyan-400" },
  { id: "to_bank", label: "To Bank", icon: Landmark, path: "/pay/send", color: "bg-amber-100 text-amber-600 dark:bg-amber-950/40 dark:text-amber-400" },
  { id: "request", label: "Request", icon: ArrowDownToLine, path: "/pay/request", color: "bg-emerald-100 text-emerald-600 dark:bg-emerald-950/40 dark:text-emerald-400" },
  { id: "add_money", label: "Add Money", icon: PlusCircle, path: "/add-money", color: "bg-orange-100 text-orange-600 dark:bg-orange-950/40 dark:text-orange-400" },
  { id: "check_balance", label: "Check Balance", icon: Eye, path: "#", color: "bg-indigo-100 text-indigo-600 dark:bg-indigo-950/40 dark:text-indigo-400" },
  { id: "transactions", label: "Transactions", icon: FileText, path: "/history", color: "bg-rose-100 text-rose-600 dark:bg-rose-950/40 dark:text-rose-400" },
];

const containerVariants = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: {
      staggerChildren: 0.05
    }
  }
};

const itemVariants = {
  hidden: { opacity: 0, scale: 0.8 },
  show: { opacity: 1, scale: 1, transition: { type: "spring" as const, stiffness: 300, damping: 24 } }
};

interface QuickActionGridProps {
  onAction: (actionId: string, path: string) => void;
}

export function QuickActionGrid({ onAction }: QuickActionGridProps) {
  return (
    <motion.div 
      className="grid grid-cols-4 gap-y-6 gap-x-4 py-6 px-2"
      variants={containerVariants}
      initial="hidden"
      animate="show"
    >
      {ACTIONS.map((action) => {
        const Icon = action.icon;
        return (
          <motion.div key={action.id} variants={itemVariants} className="flex flex-col items-center">
            <button 
              onClick={() => onAction(action.id, action.path)}
              className="flex flex-col items-center gap-2 group w-full active:scale-95 transition-transform"
            >
              <div className={`p-4 rounded-[20px] ${action.color} shadow-sm group-hover:shadow-md transition-all`}>
                <Icon size={24} strokeWidth={2} />
              </div>
              <span className="text-[11px] font-medium text-center text-foreground/80 leading-tight">
                {action.label}
              </span>
            </button>
          </motion.div>
        );
      })}
    </motion.div>
  );
}
