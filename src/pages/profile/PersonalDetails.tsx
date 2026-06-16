import { ProfileSectionLayout } from "@/components/layout/ProfileSectionLayout";
import { useAuthStore } from "@/store/authStore";
import { User, Mail, Phone, MapPin, CheckCircle2 } from "lucide-react";

export function PersonalDetails() {
  const { user } = useAuthStore();

  return (
    <ProfileSectionLayout title="Personal Details">
      <div className="flex flex-col items-center mb-8 mt-4">
        <div className="w-24 h-24 rounded-full bg-primary/10 flex items-center justify-center text-primary font-bold text-4xl border border-primary/20 mb-4 relative">
          {user?.name?.charAt(0) || "U"}
          <div className="absolute bottom-0 right-0 w-6 h-6 bg-surface rounded-full flex items-center justify-center shadow-sm">
            <CheckCircle2 size={20} className="text-success" />
          </div>
        </div>
        <h2 className="text-2xl font-bold">{user?.name}</h2>
        <p className="text-muted-foreground">PVL-{user?.id?.toString().padStart(8, '0')}</p>
      </div>

      <div className="bg-surface rounded-3xl border border-border shadow-sm overflow-hidden">
        <div className="p-4 border-b border-border flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-blue-500/10 text-blue-500 flex items-center justify-center">
            <User size={20} />
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Full Name</p>
            <p className="font-semibold">{user?.name}</p>
          </div>
        </div>
        <div className="p-4 border-b border-border flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-emerald-500/10 text-emerald-500 flex items-center justify-center">
            <Mail size={20} />
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Email Address</p>
            <p className="font-semibold">{user?.email}</p>
          </div>
        </div>
        <div className="p-4 border-b border-border flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-purple-500/10 text-purple-500 flex items-center justify-center">
            <Phone size={20} />
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Phone Number</p>
            <p className="font-semibold">+91 {user?.phone || "98765 43210"}</p>
          </div>
        </div>
        <div className="p-4 flex items-center gap-4">
          <div className="w-10 h-10 rounded-full bg-rose-500/10 text-rose-500 flex items-center justify-center">
            <MapPin size={20} />
          </div>
          <div>
            <p className="text-sm text-muted-foreground">Address</p>
            <p className="font-semibold text-sm">Chennai, Tamil Nadu, IN</p>
          </div>
        </div>
      </div>
    </ProfileSectionLayout>
  );
}
