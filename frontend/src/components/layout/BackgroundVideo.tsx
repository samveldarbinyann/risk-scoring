import { useState } from "react";
import { motion, useReducedMotion } from "motion/react";

const FADE_IN_S = 0.8;
const OPACITY = 0.3;

export function BackgroundVideo() {
  const prefersReducedMotion = useReducedMotion();
  const [isVisible, setIsVisible] = useState(false);

  if (prefersReducedMotion) return null;

  return (
    <>
      <motion.video
        aria-hidden
        className="pointer-events-none fixed inset-0 h-full w-full object-cover saturate-50 brightness-75"
        initial={{ opacity: 0 }}
        animate={{ opacity: isVisible ? OPACITY : 0 }}
        transition={{ duration: FADE_IN_S, ease: "easeInOut" }}
        src="/background-loop.mp4"
        autoPlay
        loop
        muted
        playsInline
        onPlaying={() => setIsVisible(true)}
      />
      <div aria-hidden className="pointer-events-none fixed inset-0 bg-bg/60" />
    </>
  );
}
