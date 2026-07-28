import { useCountUp } from "@/hooks/useCountUp";

interface ScoreCounterProps {
  score: number;
}

export function ScoreCounter({ score }: ScoreCounterProps) {
  const value = useCountUp(score);
  return <span className="font-mono text-5xl font-semibold tabular-nums text-text">{value}</span>;
}
