import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { useLocation, useOutlet } from "react-router";
import { NavBar } from "@/components/layout/NavBar";
import { LocaleMenu } from "@/components/layout/LocaleMenu";
import { BackgroundVideo } from "@/components/layout/BackgroundVideo";
import { SCAN_FLOW_HIDDEN, SCAN_FLOW_TRANSITION, SCAN_FLOW_VISIBLE } from "@/lib/scanFlowMotion";

const SCAN_FLOW_PATH = /^\/scan\/[^/]+(?:\/report)?\/?$/;

export function AppShell() {
  const { pathname } = useLocation();
  const outlet = useOutlet();
  const prefersReducedMotion = useReducedMotion();
  const isScanFlow = pathname === "/" || SCAN_FLOW_PATH.test(pathname);
  const hidden = prefersReducedMotion ? SCAN_FLOW_VISIBLE : SCAN_FLOW_HIDDEN;

  return (
   <div className="flex min-h-screen flex-col">
      {isScanFlow && <BackgroundVideo />}
      <NavBar />
      <main className="relative flex flex-1 flex-col">
        <LocaleMenu className="absolute right-6 top-4 z-0" />
        <AnimatePresence mode="wait">
          {isScanFlow ? (
            <motion.div
              key={pathname}
              initial={hidden}
              animate={SCAN_FLOW_VISIBLE}
              exit={hidden}
              transition={SCAN_FLOW_TRANSITION}
              className="relative flex flex-1 flex-col"
            >
              <motion.span
                aria-hidden
                initial={{ opacity: 0, scaleX: 0 }}
                animate={{ opacity: [0, 0.8, 0], scaleX: [0, 1, 0] }}
                transition={{ duration: 0.5, ease: "easeOut" }}
                className="pointer-events-none absolute inset-x-0 top-0 h-px origin-left bg-accent"
              />
              {outlet}
            </motion.div>
          ) : (
            <div key={pathname} className="flex flex-1 flex-col">
              {outlet}
            </div>
          )}
        </AnimatePresence>
      </main>
    </div>
  );
}
