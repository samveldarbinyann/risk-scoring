import { Outlet } from "react-router";
import { NavBar } from "@/components/layout/NavBar";

export function AppShell() {
  return (
    <div className="flex min-h-screen flex-col">
      <NavBar />
      <main className="flex flex-1 flex-col">
        <Outlet />
      </main>
    </div>
  );
}
