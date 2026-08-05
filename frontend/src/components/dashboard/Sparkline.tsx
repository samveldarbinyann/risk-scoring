import { cn } from "@/lib/cn";

interface SparklineProps {
  values: number[];
  startLabel: string;
  endLabel: string;
  className?: string;
}

const WIDTH = 100;
const HEIGHT = 32;
const TOP_PAD = 4;

function buildStepPoints(values: number[]): string {
  const max = Math.max(1, ...values);
  const n = values.length;
  if (n === 0) return "";

  const yFor = (value: number) => HEIGHT - (value / max) * (HEIGHT - TOP_PAD);

  if (n === 1) {
    const y = yFor(values[0]);
    return `0,${y} ${WIDTH},${y}`;
  }

  const stepX = WIDTH / (n - 1);
  const points = [`0,${yFor(values[0])}`];
  for (let i = 1; i < n; i++) {
    const x = i * stepX;
    points.push(`${x},${yFor(values[i - 1])}`, `${x},${yFor(values[i])}`);
  }
  return points.join(" ");
}

export function Sparkline({ values, startLabel, endLabel, className }: SparklineProps) {
  return (
    <div className={cn("flex flex-col gap-1", className)}>
      <svg viewBox={`0 0 ${WIDTH} ${HEIGHT}`} preserveAspectRatio="none" className="h-9 w-full text-accent">
        <line
          x1="0"
          y1={HEIGHT}
          x2={WIDTH}
          y2={HEIGHT}
          stroke="currentColor"
          strokeWidth="1"
          vectorEffect="non-scaling-stroke"
          className="text-border"
        />
        <polyline
          points={buildStepPoints(values)}
          fill="none"
          stroke="currentColor"
          strokeWidth="1.5"
          strokeLinejoin="round"
          vectorEffect="non-scaling-stroke"
        />
      </svg>
      <div className="flex justify-between font-mono text-xs text-text-faint">
        <span>{startLabel}</span>
        <span>{endLabel}</span>
      </div>
    </div>
  );
}
