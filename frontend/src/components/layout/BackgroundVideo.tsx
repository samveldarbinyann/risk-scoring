import { useState } from "react";

const FADE_IN_S = 0.8;
const OPACITY = 0.3;

export function BackgroundVideo() {
  const [isVisible, setIsVisible] = useState(false);

  return (
    <>
      <video
        className="pointer-events-none fixed inset-0 h-full w-full object-cover saturate-50 brightness-75 transition-opacity ease-in-out"
        style={{ opacity: isVisible ? OPACITY : 0, transitionDuration: `${FADE_IN_S}s` }}
        src="/background-loop.mp4"
        autoPlay
        loop
        muted
        playsInline
        onPlaying={() => setIsVisible(true)}
      />
      <div className="pointer-events-none fixed inset-0 bg-bg/60" />
    </>
  );
}
