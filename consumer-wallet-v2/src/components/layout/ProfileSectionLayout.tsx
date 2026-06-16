import type { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { PageTransition } from "@/components/animations/PageTransition";

interface ProfileSectionLayoutProps {
  title: string;
  children: ReactNode;
}

export function ProfileSectionLayout({ title, children }: ProfileSectionLayoutProps) {
  const navigate = useNavigate();

  return (
    <PageTransition className="flex flex-col h-full w-full max-w-3xl mx-auto bg-background relative pt-4 md:pt-8 md:px-8">
      {/* Header */}
      <div className="flex items-center px-4 pb-4 border-b border-border sticky top-0 bg-background/95 backdrop-blur z-10">
        <button 
          onClick={() => navigate("/profile")} 
          className="p-2 -ml-2 text-foreground active:scale-95 transition-transform"
        >
          <ArrowLeft size={24} />
        </button>
        <h2 className="flex-1 text-center font-bold text-lg md:text-2xl pr-8">{title}</h2>
      </div>

      <div className="flex-1 overflow-y-auto p-4 md:px-0 md:py-8 space-y-6">
        {children}
      </div>
    </PageTransition>
  );
}
