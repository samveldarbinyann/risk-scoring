import { useEffect, useState } from "react";
import { animate } from "motion/react";

export function useCountUp(target: number, durationSeconds = 1): number {
  const [value, setValue] = useState(0);

  useEffect(() => {
    const controls = animate(0, target, {
      duration: durationSeconds,
      ease: "easeOut",
      onUpdate: (latest) => setValue(Math.round(latest)),
    });
    return () => controls.stop();
  }, [target, durationSeconds]);

  return value;
}
