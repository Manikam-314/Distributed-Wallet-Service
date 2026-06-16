import { Link, useLocation } from "react-router-dom";
import { Home, Send, Clock, CreditCard, User } from "lucide-react";
import { cn } from "@/lib/utils";

const NAV_ITEMS = [
  { id: "home", label: "Home", icon: Home, path: "/" },
  { id: "pay", label: "Pay", icon: Send, path: "/pay" },
  { id: "history", label: "History", icon: Clock, path: "/history" },
  { id: "cards", label: "Cards", icon: CreditCard, path: "/cards" },
  { id: "profile", label: "Profile", icon: User, path: "/profile" },
];

export function MobileAppLayout({ children }: { children: React.ReactNode }) {
  const location = useLocation();

  return (
    <div className="flex flex-col md:flex-row h-[100dvh] w-full bg-background overflow-hidden relative">
      
      {/* Desktop Sidebar / Mobile Bottom Nav */}
      <aside className="fixed md:static bottom-0 w-full md:w-64 bg-surface/90 md:bg-surface md:shadow-md backdrop-blur-lg md:backdrop-blur-none border-t md:border-t-0 md:border-r border-border pt-2 md:pt-8 pb-safe md:pb-8 px-4 md:px-0 z-50 flex md:flex-col shrink-0 order-2 md:order-1 transition-all">
        {/* Brand/Logo for Desktop */}
        <div className="hidden md:flex items-center gap-3 px-8 mb-10">
          <div className="w-10 h-10 rounded-xl bg-primary flex items-center justify-center text-primary-foreground font-bold text-xl shadow-md">
            W
          </div>
          <span className="font-bold text-xl tracking-tight text-foreground">DistriWallet</span>
        </div>

        <nav className="flex md:flex-col justify-around md:justify-start items-center md:items-stretch h-16 w-full max-w-4xl md:max-w-none mx-auto gap-1 md:gap-2 md:px-4">
          {NAV_ITEMS.map((item) => {
            const isActive = location.pathname === item.path || (item.path !== '/' && location.pathname.startsWith(item.path));
            const Icon = item.icon;

            return (
              <Link
                key={item.id}
                to={item.path}
                className={cn(
                  "flex flex-col md:flex-row items-center md:justify-start justify-center w-16 md:w-full h-full md:h-12 gap-1 md:gap-4 md:px-4 md:rounded-xl active:scale-95 transition-all text-sm group",
                  isActive ? "md:bg-primary/10" : "hover:md:bg-accent"
                )}
                role="button"
                aria-label={`Navigate to ${item.label}`}
              >
                <div
                  className={cn(
                    "flex items-center justify-center p-1.5 md:p-0 rounded-full transition-colors",
                    isActive ? "bg-primary/10 md:bg-transparent text-primary" : "text-muted group-hover:text-foreground"
                  )}
                >
                  <Icon size={24} strokeWidth={isActive ? 2.5 : 2} className="md:w-5 md:h-5" />
                </div>
                <span
                  className={cn(
                    "text-[10px] md:text-sm font-medium transition-colors md:block hidden md:flex-1 text-left",
                    isActive ? "text-primary font-bold" : "text-muted group-hover:text-foreground"
                  )}
                >
                  {item.label}
                </span>
                <span
                  className={cn(
                    "text-[10px] font-medium transition-colors md:hidden",
                    isActive ? "text-primary" : "text-muted"
                  )}
                >
                  {item.label}
                </span>
              </Link>
            );
          })}
        </nav>
      </aside>

      {/* Main Content Area */}
      <main className="flex-1 overflow-y-auto overflow-x-hidden pb-20 md:pb-0 scroll-smooth bg-background order-1 md:order-2 flex justify-center">
        <div className="w-full max-w-[1400px] mx-auto pt-0 md:pt-4">
          {children}
        </div>
      </main>
    </div>
  );
}
