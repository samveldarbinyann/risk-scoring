import { useEffect, useRef, useState } from "react";

export function useCooldown(): { cooldown: number; start: (seconds: number) => void } {
  const [cooldown, setCooldown] = useState(0);
  const isActive = cooldown > 0;

  useEffect(() => {
    if (!isActive) return;
    const timer = setInterval(() => setCooldown((seconds) => Math.max(0, seconds - 1)), 1000);
    return () => clearInterval(timer);
  }, [isActive]);

  const startRef = useRef((seconds: number) => setCooldown(seconds));
  return { cooldown, start: startRef.current };
}
