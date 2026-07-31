import { CodeBlock } from "@/components/ui/CodeBlock";

interface ApiEndpointProps {
  title: string;
  description: string;
  code: string;
}

export function ApiEndpoint({ title, description, code }: ApiEndpointProps) {
  return (
    <div className="flex flex-col gap-3">
      <h3 className="font-sans text-sm font-medium text-text">{title}</h3>
      <p className="max-w-2xl text-sm leading-relaxed text-text-dim">{description}</p>
      <CodeBlock code={code} />
    </div>
  );
}
