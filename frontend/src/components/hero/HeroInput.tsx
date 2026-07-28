interface HeroInputProps {
  value: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
  placeholder: string;
  disabled?: boolean;
}

export function HeroInput({ value, onChange, onSubmit, placeholder, disabled }: HeroInputProps) {
  return (
    <div
      className="flex w-full max-w-2xl items-center gap-3 rounded-base border border-border
                    bg-surface px-6 py-4 font-mono transition-[border-color,box-shadow]
                    focus-within:border-accent focus-within:shadow-[0_0_0_1px_var(--color-accent),0_0_28px_-6px_var(--color-accent)]"
    >
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => event.key === "Enter" && onSubmit()}
        disabled={disabled}
        placeholder={placeholder}
        className="flex-1 bg-transparent text-lg text-text outline-none placeholder:text-text-faint disabled:text-text-faint"
      />
    </div>
  );
}
