import { useEffect, useState, type FormEvent } from "react";
import { Navigate, NavLink, useNavigate } from "react-router";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";

const USERNAME_PATTERN = /^[A-Za-z0-9_]{3,32}$/;
const PASSWORD_PATTERN = /^(?=.*[A-Za-z])(?=.*\d).*$/;
const MIN_PASSWORD_LENGTH = 12;
const RESEND_COOLDOWN_SECONDS = 30;
const VERIFICATION_CODE_LENGTH = 6;

type Step = "register" | "verify";

export function RegisterPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const { status, register, verifyEmail, resendCode } = useAuth();

  const [step, setStep] = useState<Step>("register");
  const [username, setUsername] = useState("");
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [cooldown, setCooldown] = useState(0);

  useEffect(() => {
    if (cooldown <= 0) return;
    const timer = setInterval(() => setCooldown((seconds) => seconds - 1), 1000);
    return () => clearInterval(timer);
  }, [cooldown]);

  if (status === "authenticated") {
    return <Navigate to="/" replace />;
  }

  async function handleRegister(event: FormEvent) {
    event.preventDefault();
    if (isSubmitting) return;

    if (password !== confirmPassword) {
      setError(t("auth.passwordMismatch"));
      return;
    }

    setError(null);
    setIsSubmitting(true);
    try {
      await register({
        username: username.trim(),
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
      });
      setStep("verify");
      setCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleVerify(event: FormEvent) {
    event.preventDefault();
    if (isSubmitting) return;

    setError(null);
    setIsSubmitting(true);
    try {
      await verifyEmail(email.trim(), code.trim());
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
      await resendCode(email.trim());
      setCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    }
  }

  if (step === "verify") {
    return (
      <div className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center gap-6 px-6 py-10">
        <Card title={t("auth.verifyTitle")} className="w-full">
          <form className="flex flex-col gap-4" onSubmit={handleVerify}>
            <p className="font-mono text-sm text-text-dim">
              {t("auth.verifySubtitle")} <span className="text-text">{email}</span>
            </p>
            <Input
              value={code}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, VERIFICATION_CODE_LENGTH))}
              placeholder={t("auth.verifyCodeField")}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={VERIFICATION_CODE_LENGTH}
              required
            />
            <ErrorMessage message={error} size="sm" />
            <Button type="submit" isLoading={isSubmitting} className="w-full">
              {t("auth.verifySubmit")}
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
      <Card title={t("auth.registerTitle")} className="w-full">
        <form className="flex flex-col gap-4" onSubmit={handleRegister}>
          <Input
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            placeholder={t("auth.usernameField")}
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            pattern={USERNAME_PATTERN.source}
            minLength={3}
            maxLength={32}
            required
          />
          <div className="grid grid-cols-2 gap-4">
            <Input
              value={firstName}
              onChange={(event) => setFirstName(event.target.value)}
              placeholder={t("auth.firstNameField")}
              autoComplete="given-name"
              maxLength={64}
              required
            />
            <Input
              value={lastName}
              onChange={(event) => setLastName(event.target.value)}
              placeholder={t("auth.lastNameField")}
              autoComplete="family-name"
              maxLength={64}
              required
            />
          </div>
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
          <Input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={t("auth.passwordField")}
            autoComplete="new-password"
            minLength={MIN_PASSWORD_LENGTH}
            maxLength={128}
            required
          />
          <Input
            type="password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            placeholder={t("auth.confirmPasswordField")}
            autoComplete="new-password"
            minLength={MIN_PASSWORD_LENGTH}
            maxLength={128}
            required
          />
          <ErrorMessage message={error} size="sm" />
          <Button type="submit" isLoading={isSubmitting} className="w-full">
            {t("auth.registerSubmit")}
          </Button>
        </form>
      </Card>
      <p className="font-mono text-sm text-text-dim">
        {t("auth.haveAccount")}{" "}
        <NavLink to="/auth" className="text-accent hover:text-accent-press">
          {t("auth.loginLink")}
        </NavLink>
      </p>
    </div>
  );
}
