import type { ReactNode } from "react";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";

interface CardStateProps {
  isLoading: boolean;
  error: string | null;
  children: ReactNode;
}

export function CardState({ isLoading, error, children }: CardStateProps) {
  if (isLoading) {
    return (
      <div className="flex justify-center py-8">
        <Spinner />
      </div>
    );
  }

  if (error) {
    return <ErrorMessage message={error} size="sm" />;
  }

  return <>{children}</>;
}
