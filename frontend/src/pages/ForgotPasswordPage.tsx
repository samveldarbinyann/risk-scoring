import { useState, type FormEvent } from "react";
import { Navigate, NavLink, useNavigate } from "react-router";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";
import { useCooldown } from "@/hooks/useCooldown";
import { MAX_PASSWORD_LENGTH, MIN_PASSWORD_LENGTH, PASSWORD_PATTERN } from "@/lib/validation";

const RESEND_COOLDOWN_SECONDS = 30;
const RESET_CODE_LENGTH = 6;

type Step = "request" | "reset";

export function ForgotPasswordPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const { status, forgotPassword, resetPassword } = useAuth();

  const [step, setStep] = useState<Step>("request");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const { cooldown, start: startCooldown } = useCooldown();

  if (status === "authenticated") {
    return <Navigate to="/" replace />;
  }

  async function handleRequest(event: FormEvent) {
    event.preventDefault();
    if (isSubmitting) return;

    setError(null);
    setIsSubmitting(true);
    try {
      await forgotPassword(email.trim());
      setStep("reset");
      startCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleReset(event: FormEvent) {
    event.preventDefault();
    if (isSubmitting) return;

    if (newPassword !== confirmPassword) {
      setError(t("auth.passwordMismatch"));
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      await resetPassword(email.trim(), code.trim(), newPassword);
      navigate("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleResend() {
    if (cooldown > 0) return;

    setError(null);
    try {
      await forgotPassword(email.trim());
      startCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  if (step === "reset") {
    return (
      <div className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center gap-6 px-6 py-10">
        <Card title={t("auth.resetPasswordTitle")} className="w-full">
          <form className="flex flex-col gap-4" onSubmit={handleReset}>
            <p className="font-mono text-sm text-text-dim">
              {t("auth.verifySubtitle")} <span className="text-text">{email}</span>
            </p>
            <Input
              value={code}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, RESET_CODE_LENGTH))}
              placeholder={t("auth.verifyCodeField")}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={RESET_CODE_LENGTH}
              required
            />
            <Input
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              placeholder={t("auth.newPasswordField")}
              autoComplete="new-password"
              minLength={MIN_PASSWORD_LENGTH}
              maxLength={MAX_PASSWORD_LENGTH}
              pattern={PASSWORD_PATTERN.source}
              required
            />
            <Input
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              placeholder={t("auth.confirmPasswordField")}
              autoComplete="new-password"
              minLength={MIN_PASSWORD_LENGTH}
              maxLength={MAX_PASSWORD_LENGTH}
              required
            />
            <ErrorMessage message={error} size="sm" />
            <Button type="submit" isLoading={isSubmitting} className="w-full">
              {t("auth.resetPasswordSubmit")}
            </Button>
            <Button type="button" variant="ghost" disabled={cooldown > 0} onClick={handleResend} className="w-full">
              {cooldown > 0 ? `${t("auth.resendCode")} (${cooldown}s)` : t("auth.resendCode")}
            </Button>
          </form>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center gap-6 px-6 py-10">
      <Card title={t("auth.forgotPasswordTitle")} className="w-full">
        <form className="flex flex-col gap-4" onSubmit={handleRequest}>
          <p className="font-mono text-sm text-text-dim">{t("auth.forgotPasswordIntro")}</p>
          <Input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder={t("auth.emailField")}
            autoComplete="email"
            autoCapitalize="none"
            autoCorrect="off"
            maxLength={320}
            required
          />
          <ErrorMessage message={error} size="sm" />
          <Button type="submit" isLoading={isSubmitting} className="w-full">
            {t("auth.forgotPasswordSubmit")}
          </Button>
        </form>
      </Card>
      <p className="font-mono text-sm text-text-dim">
        <NavLink to="/auth" className="text-accent hover:text-accent-press">
          {t("auth.backToLogin")}
        </NavLink>
      </p>
    </div>
  );
}
