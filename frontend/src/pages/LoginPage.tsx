import { useState, type FormEvent } from "react";
import { Navigate, useNavigate } from "react-router";
import { NavLink } from "react-router";
import { Card } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { useAuth } from "@/lib/auth/context";
import { useI18n } from "@/lib/i18n/context";

export function LoginPage() {
  const { t } = useI18n();
  const navigate = useNavigate();
  const { status, login } = useAuth();
  const [loginValue, setLoginValue] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (status === "authenticated") {
    return <Navigate to="/" replace />;
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (isSubmitting) return;

    setError(null);
    setIsSubmitting(true);
    try {
      await login({ login: loginValue.trim(), password });
      navigate("/");
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-md flex-1 flex-col items-center justify-center gap-6 px-6 py-10">
      <Card title={t("auth.loginTitle")} className="w-full">
        <form className="flex flex-col gap-4" onSubmit={handleSubmit}>
          <Input
            value={loginValue}
            onChange={(event) => setLoginValue(event.target.value)}
            placeholder={t("auth.loginField")}
            autoComplete="username"
            autoCapitalize="none"
            autoCorrect="off"
            required
          />
          <Input
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={t("auth.passwordField")}
            autoComplete="current-password"
            required
          />
          <NavLink to="/forgot-password" className="self-end font-mono text-xs text-text-dim hover:text-accent">
            {t("auth.forgotPasswordLink")}
          </NavLink>
          <ErrorMessage message={error} size="sm" />
          <Button type="submit" isLoading={isSubmitting} className="w-full">
            {t("auth.loginSubmit")}
          </Button>
        </form>
      </Card>
      <p className="font-mono text-sm text-text-dim">
        {t("auth.noAccount")}{" "}
        <NavLink to="/register" className="text-accent hover:text-accent-press">
          {t("auth.registerLink")}
        </NavLink>
      </p>
    </div>
  );
}
