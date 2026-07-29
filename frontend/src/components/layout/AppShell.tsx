import { Outlet, useLocation } from "react-router";
import { NavBar } from "@/components/layout/NavBar";
import { LocaleMenu } from "@/components/layout/LocaleMenu";
import { BackgroundVideo } from "@/components/layout/BackgroundVideo";

const LANDING_PATH = "/";

export function AppShell() {
  const { pathname } = useLocation();

  return (
    <div className="flex min-h-screen flex-col">
      {pathname === LANDING_PATH && <BackgroundVideo />}
      <NavBar />
      <main className="relative flex flex-1 flex-col">
        <LocaleMenu className="absolute right-6 top-4 z-40" />
        <Outlet />
      </main>
    </div>
  );
}
