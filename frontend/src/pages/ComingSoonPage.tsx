import { useI18n } from "@/lib/i18n/context";
import type { MessageKey } from "@/lib/i18n/messageKeys";

interface ComingSoonPageProps {
  titleKey: MessageKey;
}

export function ComingSoonPage({ titleKey }: ComingSoonPageProps) {
  const { t } = useI18n();

  return (
    <div className="mx-auto flex w-full max-w-3xl flex-1 flex-col items-center justify-center gap-2 px-6 py-10 text-center">
      <p className="font-sans text-xs uppercase tracking-widest text-text-dim">{t(titleKey)}</p>
      <p className="text-sm text-text-faint">{t("comingSoon.body")}</p>
    </div>
  );
}
