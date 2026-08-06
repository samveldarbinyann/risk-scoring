import { useEffect, useRef, type RefObject } from "react";

export function useDismissableMenu<T extends HTMLElement>(isOpen: boolean, onDismiss: () => void): RefObject<T | null> {
  const containerRef = useRef<T>(null);

  useEffect(() => {
    if (!isOpen) return;

    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        onDismiss();
      }
    }

    function handleEscape(event: KeyboardEvent) {
      if (event.key === "Escape") onDismiss();
    }

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [isOpen, onDismiss]);

  return containerRef;
}
