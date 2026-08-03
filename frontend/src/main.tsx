import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App.tsx";
import { I18nProvider } from "@/lib/i18n/I18nProvider";
import { AuthProvider } from "@/lib/auth/AuthProvider";
import { ChainRegistryProvider } from "@/lib/chains/ChainRegistryProvider";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <I18nProvider>
      <AuthProvider>
        <ChainRegistryProvider>
          <App />
        </ChainRegistryProvider>
      </AuthProvider>
    </I18nProvider>
  </StrictMode>,
);
