import { create } from "zustand";
import { persist } from "zustand/middleware";

interface SettingsState {
  biometrics: boolean;
  hideBalance: boolean;
  twoFactor: boolean;
  notifications: boolean;
  emailAlerts: boolean;
  darkMode: boolean;
  setBiometrics: (val: boolean) => void;
  setHideBalance: (val: boolean) => void;
  setTwoFactor: (val: boolean) => void;
  setNotifications: (val: boolean) => void;
  setEmailAlerts: (val: boolean) => void;
  setDarkMode: (val: boolean) => void;
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      biometrics: true,
      hideBalance: false,
      twoFactor: true,
      notifications: true,
      emailAlerts: false,
      darkMode: false,
      setBiometrics: (val) => set({ biometrics: val }),
      setHideBalance: (val) => set({ hideBalance: val }),
      setTwoFactor: (val) => set({ twoFactor: val }),
      setNotifications: (val) => set({ notifications: val }),
      setEmailAlerts: (val) => set({ emailAlerts: val }),
      setDarkMode: (val) => {
        set({ darkMode: val });
        if (val) {
          document.documentElement.classList.add("dark");
        } else {
          document.documentElement.classList.remove("dark");
        }
      },
    }),
    {
      name: "settings-storage",
    }
  )
);
