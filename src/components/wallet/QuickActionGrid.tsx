import { motion } from "framer-motion";
import { Send, ArrowDownToLine, QrCode, PlusCircle, FileText, Smartphone, Users, MapPin } from "lucide-react";
import { Link } from "react-router-dom";

const ACTIONS = [
  { id: "send", label: "Send Money", icon: Send, path: "/pay/send", color: "bg-blue-100 text-blue-600" },
  { id: "request", label: "Request", icon: ArrowDownToLine, path: "/pay/request", color: "bg-emerald-100 text-emerald-600" },
  { id: "scan", label: "Scan QR", icon: QrCode, path: "/pay/scan", color: "bg-purple-100 text-purple-600" },
  { id: "add", label: "Add Money", icon: PlusCircle, path: "/add-money", color: "bg-orange-100 text-orange-600" },
  { id: "bills", label: "Pay Bills", icon: FileText, path: "/bills", color: "bg-rose-100 text-rose-600" },
  { id: "recharge", label: "Recharge", icon: Smartphone, path: "/recharge", color: "bg-cyan-100 text-cyan-600" },
  { id: "split", label: "Split Bill", icon: Users, path: "/split", color: "bg-indigo-100 text-indigo-600" },
  { id: "nearby", label: "Nearby", icon: MapPin, path: "/nearby", color: "bg-pink-100 text-pink-600" },
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

export function QuickActionGrid() {
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
            <Link 
              to={action.path}
              className="flex flex-col items-center gap-2 group w-full active:scale-95 transition-transform"
            >
              <div className={`p-4 rounded-[20px] ${action.color} shadow-sm group-hover:shadow-md transition-all`}>
                <Icon size={24} strokeWidth={2} />
              </div>
              <span className="text-[11px] font-medium text-center text-foreground/80 leading-tight">
                {action.label}
              </span>
            </Link>
          </motion.div>
        );
      })}
    </motion.div>
  );
}
