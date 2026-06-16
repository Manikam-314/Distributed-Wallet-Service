import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function formatBackendDate(ts: any): Date {
  if (!ts) return new Date();
  if (Array.isArray(ts)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = ts;
    return new Date(year, month - 1, day, hour, minute, second);
  }
  return new Date(ts);
}
