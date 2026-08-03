import { useState } from "react";
import { useSearchParams } from "react-router";
import { ContactForm } from "@/components/contact/ContactForm";
import { Button } from "@/components/ui/Button";
import { Card } from "@/components/ui/Card";
import { submitContact } from "@/lib/api";
import { useI18n } from "@/lib/i18n/context";

export function ContactPage() {
  const { t } = useI18n();
  const [searchParams] = useSearchParams();
  const scanId = searchParams.get("scanId");

  const [email, setEmail] = useState("");
  const [subject, setSubject] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isSent, setIsSent] = useState(false);

  async function handleSubmit() {
    if (isSubmitting) return;

    setError(null);
    setIsSubmitting(true);
    try {
      await submitContact({
        email: email.trim(),
        subject: subject.trim(),
        message: message.trim(),
        scanId,
      });
      setIsSent(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("contact.error"));
    } finally {
      setIsSubmitting(false);
    }
  }

  function handleReset() {
    setSubject("");
    setMessage("");
    setError(null);
    setIsSent(false);
  }

  return (
    <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col gap-8 px-6 py-10">
      <header className="flex flex-col gap-3">
        <h1 className="font-sans text-3xl font-semibold text-text sm:text-4xl">{t("contact.title")}</h1>
        <p className="text-sm leading-relaxed text-accent">{t("contact.subtitle")}</p>
      </header>

      <Card>
        {isSent ? (
          <div className="flex flex-col items-start gap-4">
            <div className="flex flex-col gap-2">
              <h2 className="font-mono text-sm uppercase tracking-widest text-risk-low">{t("contact.sentTitle")}</h2>
              <p className="text-sm leading-relaxed text-text-dim">{t("contact.sentBody")}</p>
            </div>
            <Button type="button" variant="ghost" onClick={handleReset}>
              {t("contact.sendAnother")}
            </Button>
          </div>
        ) : (
          <ContactForm
            email={email}
            subject={subject}
            message={message}
            scanId={scanId}
            error={error}
            isSubmitting={isSubmitting}
            onEmailChange={setEmail}
            onSubjectChange={setSubject}
            onMessageChange={setMessage}
            onSubmit={() => void handleSubmit()}
          />
        )}
      </Card>
    </div>
  );
}
