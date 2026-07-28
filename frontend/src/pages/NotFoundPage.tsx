import { useI18n } from "@/lib/i18n/context";

export function NotFoundPage() {
  const { t } = useI18n();

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center gap-2 px-6 py-10 text-center">
      <p className="font-mono text-sm text-text-dim">{t("notFound.title")}</p>
      <p className="text-sm text-text-faint">{t("notFound.body")}</p>
    </div>
  );
}
