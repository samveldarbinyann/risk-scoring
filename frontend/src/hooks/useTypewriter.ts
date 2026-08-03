import { useEffect, useRef, useState } from "react";
import { animate } from "motion/react";

export const TYPEWRITER_MS_PER_CHAR = 12;

export function typewriterDurationMs(text: string, msPerChar = TYPEWRITER_MS_PER_CHAR): number {
  return text.length * msPerChar;
}

interface TypewriterState {
  text: string;
  isTyping: boolean;
}

export function useTypewriter(text: string, delayMs = 0, msPerChar = TYPEWRITER_MS_PER_CHAR): TypewriterState {
  const [charCount, setCharCount] = useState(0);
  const [isTyping, setIsTyping] = useState(false);
  const prevTextRef = useRef(text);

  useEffect(() => {
    if (!text) return;

    if (prevTextRef.current !== text) {
      prevTextRef.current = text;
      setCharCount(0);
      setIsTyping(false);
    }

    let controls: { stop: () => void } | undefined;
    const timeout = setTimeout(() => {
      setIsTyping(true);
      controls = animate(0, text.length, {
        duration: typewriterDurationMs(text, msPerChar) / 1000,
        ease: "linear",
        onUpdate: (latest) => setCharCount(Math.floor(latest)),
        onComplete: () => setIsTyping(false),
      });
    }, delayMs);

    return () => {
      clearTimeout(timeout);
      controls?.stop();
    };
  }, [text, delayMs, msPerChar]);

  return { text: text.slice(0, charCount), isTyping };
}
