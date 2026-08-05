import type { FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Input } from "@/components/ui/Input";
import { Textarea } from "@/components/ui/Textarea";
import { truncateId } from "@/lib/format";
import { useI18n } from "@/lib/i18n/context";

interface ContactFormProps {
  email: string;
  subject: string;
  message: string;
  scanId: string | null;
  error: string | null;
  isSubmitting: boolean;
  onEmailChange: (value: string) => void;
  onSubjectChange: (value: string) => void;
  onMessageChange: (value: string) => void;
  onSubmit: () => void;
}

const MESSAGE_MAX_LENGTH = 5000;
const SUBJECT_MAX_LENGTH = 255;

export function ContactForm({
  email,
  subject,
  message,
  scanId,
  error,
  isSubmitting,
  onEmailChange,
  onSubjectChange,
  onMessageChange,
  onSubmit,
}: ContactFormProps) {
  const { t } = useI18n();

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    onSubmit();
  }

  return (
    <form className="flex h-full flex-col gap-4" onSubmit={handleSubmit}>
      <Input
        type="email"
        value={email}
        onChange={(event) => onEmailChange(event.target.value)}
        placeholder={t("contact.emailField")}
        autoComplete="email"
        autoCapitalize="none"
        autoCorrect="off"
        maxLength={320}
        required
      />
      <Input
        value={subject}
        onChange={(event) => onSubjectChange(event.target.value)}
        placeholder={t("contact.subjectField")}
        maxLength={SUBJECT_MAX_LENGTH}
        required
      />
      <Textarea
        value={message}
        onChange={(event) => onMessageChange(event.target.value)}
        placeholder={t("contact.messageField")}
        maxLength={MESSAGE_MAX_LENGTH}
        rows={8}
        required
        className="flex-1 resize-none"
      />

      {scanId && (
        <p className="font-mono text-xs text-text-faint">
          {t("contact.relatedScan")} <span className="text-text-dim">{truncateId(scanId)}</span>
        </p>
      )}

      <ErrorMessage message={error} size="sm" />

      <Button type="submit" isLoading={isSubmitting} className="sm:w-auto sm:self-end">
        {t("contact.submit")}
      </Button>
    </form>
  );
}
