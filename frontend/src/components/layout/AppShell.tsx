import { Outlet } from "react-router";
import { NavBar } from "@/components/layout/NavBar";
import { LocaleMenu } from "@/components/layout/LocaleMenu";
import { BackgroundVideo } from "@/components/layout/BackgroundVideo";

export function AppShell() {
  return (
    <div className="flex min-h-screen flex-col">
      <BackgroundVideo />
      <NavBar />
      <main className="relative flex flex-1 flex-col">
        <LocaleMenu className="absolute right-6 top-4 z-40" />
        <Outlet />
      </main>
    </div>
  );
}
