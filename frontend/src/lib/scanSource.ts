import type { ScanSource } from "@/lib/types";
import type { MessageKey } from "@/lib/i18n/messageKeys";

export const SCAN_SOURCE: Record<ScanSource, { labelKey: MessageKey }> = {
  USER: { labelKey: "dashboard.history.source.USER" },
  API: { labelKey: "dashboard.history.source.API" },
  MONITOR: { labelKey: "dashboard.history.source.MONITOR" },
};
