import { useEffect, useState } from "react";
import { animate } from "motion/react";

export const TYPEWRITER_MS_PER_CHAR = 12;

export function typewriterDurationMs(text: string): number {
  return text.length * TYPEWRITER_MS_PER_CHAR;
}

interface TypewriterState {
  text: string;
  isTyping: boolean;
}

export function useTypewriter(text: string, delayMs = 0): TypewriterState {
  const [charCount, setCharCount] = useState(0);
  const [isTyping, setIsTyping] = useState(false);
  const [prevText, setPrevText] = useState(text);

  if (prevText !== text) {
    setPrevText(text);
    setCharCount(0);
    setIsTyping(false);
  }

  useEffect(() => {
    if (!text) return;

    let controls: { stop: () => void } | undefined;
    const timeout = setTimeout(() => {
      setIsTyping(true);
      controls = animate(0, text.length, {
        duration: typewriterDurationMs(text) / 1000,
        ease: "linear",
        onUpdate: (latest) => setCharCount(Math.floor(latest)),
        onComplete: () => setIsTyping(false),
      });
    }, delayMs);

    return () => {
      clearTimeout(timeout);
      controls?.stop();
    };
  }, [text, delayMs]);

  return { text: text.slice(0, charCount), isTyping };
}
