import { useEffect, useRef, useState } from "react";
import { motion, useReducedMotion } from "motion/react";

const FADE_IN_S = 0.8;
const OPACITY = 0.3;
const VIDEO_SRC = "/backround.mp4";
const POSTER_SRC = "/backround-poster.jpg";
const LAYER_CLASS = "pointer-events-none fixed inset-0 h-full w-full object-cover";

function Scrim() {
  return <div aria-hidden className="pointer-events-none fixed inset-0 bg-bg/60" />;
}

export function BackgroundVideo() {
  const prefersReducedMotion = useReducedMotion();
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    video.muted = true;
    video.setAttribute("muted", "");
    video.play().catch(() => setIsVisible(true));
  }, []);

  if (prefersReducedMotion) {
    return (
      <>
        <img aria-hidden alt="" src={POSTER_SRC} className={LAYER_CLASS} style={{ opacity: OPACITY }} />
        <Scrim />
      </>
    );
  }

  return (
    <>
      <motion.video
        ref={videoRef}
        aria-hidden
        className={LAYER_CLASS}
        initial={{ opacity: 0 }}
        animate={{ opacity: isVisible ? OPACITY : 0 }}
        transition={{ duration: FADE_IN_S, ease: "easeInOut" }}
        src={VIDEO_SRC}
        poster={POSTER_SRC}
        autoPlay
        loop
        muted
        playsInline
        onLoadedData={() => setIsVisible(true)}
        onPlaying={() => setIsVisible(true)}
      />
      <Scrim />
    </>
  );
}
