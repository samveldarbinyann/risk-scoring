export interface DocsTerm {
  term: string;
  description: string;
}

interface DocsTermListProps {
  items: DocsTerm[];
  numbered?: boolean;
  columns?: 2 | 3;
}

const COLUMN_CLASSES: Record<2 | 3, string> = {
  2: "sm:grid-cols-2",
  3: "sm:grid-cols-3",
};

export function DocsTermList({ items, numbered = false, columns = 2 }: DocsTermListProps) {
  return (
    <dl className={`grid gap-4 ${COLUMN_CLASSES[columns]}`}>
      {items.map((item, index) => (
        <div key={item.term} className="rounded-panel border border-border bg-surface p-5">
          <dt className="flex items-baseline gap-2 font-sans text-sm font-medium text-text">
            {numbered && <span className="font-mono text-xs text-accent">{index + 1}</span>}
            {item.term}
          </dt>
          <dd className="mt-2 text-sm leading-relaxed text-text-dim">{item.description}</dd>
        </div>
      ))}
    </dl>
  );
}
