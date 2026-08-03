import { createElement } from "react";
import { TypewriterCaret } from "@/components/ui/TypewriterCaret";
import { useTypewriter } from "@/hooks/useTypewriter";

interface TypewriterTextProps {
  text: string;
  delayMs?: number;
  msPerChar?: number;
  as?: "h1" | "p" | "span";
  className?: string;
}

export function TypewriterText({ text, delayMs = 0, msPerChar, as = "p", className }: TypewriterTextProps) {
  const { text: displayed, isTyping } = useTypewriter(text, delayMs, msPerChar);

  return createElement(
    as,
    { className },
    <>
      {displayed}
      {isTyping && <TypewriterCaret />}
    </>,
  );
}
