import { useCallback, useEffect, useState } from "react";
import { motion, type Variants } from "motion/react";
import { Navigate } from "react-router";
import { PortfolioHeroCard } from "@/components/dashboard/PortfolioHeroCard";
import { WatchlistEntryList } from "@/components/watchlist/WatchlistEntryList";
import { WatchlistForm } from "@/components/watchlist/WatchlistForm";
import { Card } from "@/components/ui/Card";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Spinner } from "@/components/ui/Spinner";
import { addToWatchlist, listWatchlist, removeFromWatchlist } from "@/lib/api";
import { useAuth } from "@/lib/auth/context";
import { useChains } from "@/lib/chains/context";
import type { Chain } from "@/lib/chains/registry";
import { useI18n } from "@/lib/i18n/context";
import { pollUntil } from "@/lib/poll";
import type { WatchlistEntryView } from "@/lib/types";

const GRID_VARIANTS: Variants = {
  hidden: {},
  show: { transition: { staggerChildren: 0.06 } },
};

const SECTION_VARIANTS: Variants = {
  hidden: { opacity: 0, y: 4 },
  show: { opacity: 1, y: 0, transition: { duration: 0.18, ease: "easeOut" } },
};

export function WatchlistPage() {
  const { t } = useI18n();
  const { status } = useAuth();
  const { defaultChain } = useChains();
  const [entries, setEntries] = useState<WatchlistEntryView[]>([]);
  const [address, setAddress] = useState("");
  const [selectedChain, setSelectedChain] = useState<Chain | null>(null);
  const chain = selectedChain ?? defaultChain;
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [removingId, setRemovingId] = useState<string | null>(null);
  const [page, setPage] = useState(0);

  const loadEntries = useCallback(async () => {
    const data = await listWatchlist();
    setEntries(data);
    return data;
  }, []);

  const refresh = useCallback(async () => {
    setLoadError(null);
    setIsLoading(true);
    try {
      await loadEntries();
    } catch (err) {
      setLoadError(err instanceof Error ? err.message : t("watchlist.loadError"));
    } finally {
      setIsLoading(false);
    }
  }, [loadEntries, t]);

  useEffect(() => {
    if (status !== "authenticated") return;
    void refresh();
  }, [status, refresh]);

  if (status === "loading") {
    return (
      <div className="mx-auto flex w-full max-w-4xl flex-1 flex-col items-center justify-center px-6 py-10">
        <Spinner />
      </div>
    );
  }

  if (status === "unauthenticated") {
    return <Navigate to="/auth" replace />;
  }

  async function handleAdd() {
    if (isSubmitting) return;

    const trimmed = address.trim();
    if (!trimmed || chain === null) {
      setActionError(t("watchlist.invalidAddress"));
      setStatusMessage(null);
      return;
    }

    setActionError(null);
    setStatusMessage(null);
    setIsSubmitting(true);

    try {
      await addToWatchlist({ address: trimmed, chain });
      const { value, matched } = await pollUntil(
        loadEntries,
        (list) => list.some((entry) => entry.address.toLowerCase() === trimmed.toLowerCase() && entry.chain === chain),
      );
      setEntries(value);
      setPage(0);
      if (matched) {
        setAddress("");
      } else {
        setStatusMessage(t("watchlist.acceptedPending"));
      }
    } catch (err) {
      setActionError(err instanceof Error ? err.message : t("watchlist.addError"));
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleRemove(id: string) {
    if (removingId) return;

    setActionError(null);
    setStatusMessage(null);
    setRemovingId(id);

    try {
      await removeFromWatchlist(id);
      const { value, matched } = await pollUntil(
        loadEntries,
        (list) => list.every((entry) => entry.id !== id),
      );
      setEntries(value);
      setPage(0);
      if (!matched) {
        setStatusMessage(t("watchlist.acceptedPending"));
      }
    } catch (err) {
      setActionError(err instanceof Error ? err.message : t("watchlist.removeError"));
    } finally {
      setRemovingId(null);
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-1 flex-col gap-6 px-6 py-10">
      <h1 className="font-sans text-xs uppercase tracking-widest text-text-dim">{t("watchlist.title")}</h1>

      <motion.div variants={GRID_VARIANTS} initial="hidden" animate="show" className="flex flex-col gap-6">
        <motion.div variants={SECTION_VARIANTS}>
          <PortfolioHeroCard entries={entries} isLoading={isLoading} error={loadError} showCta={false} />
        </motion.div>

        <motion.div variants={SECTION_VARIANTS}>
          <Card title={t("watchlist.addTitle")}>
            <WatchlistForm
              address={address}
              chain={chain}
              isSubmitting={isSubmitting}
              onAddressChange={setAddress}
              onChainChange={setSelectedChain}
              onSubmit={() => void handleAdd()}
            />
            <div className="mt-3 flex flex-col gap-2">
              <ErrorMessage message={actionError} size="sm" />
              {statusMessage && <p className="font-mono text-sm text-text-dim">{statusMessage}</p>}
            </div>
          </Card>
        </motion.div>

        <motion.div variants={SECTION_VARIANTS}>
          <WatchlistEntryList
            entries={entries}
            page={page}
            isLoading={isLoading}
            error={loadError}
            removingId={removingId}
            onPageChange={setPage}
            onRemove={(id) => void handleRemove(id)}
          />
        </motion.div>
      </motion.div>
    </div>
  );
}
