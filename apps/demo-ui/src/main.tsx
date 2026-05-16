import React from "react";
import ReactDOM from "react-dom/client";
import { GoogleOAuthProvider } from "@react-oauth/google";
import App from "./App";
import { AuthProvider } from "./auth/AuthContext";
import { appConfig } from "./config/appConfig";
import "./styles/global.css";

const app = (
  <React.StrictMode>
    <AuthProvider>
      <App />
    </AuthProvider>
  </React.StrictMode>
);

ReactDOM.createRoot(document.getElementById("root")!).render(
  appConfig.authMode === "google" && appConfig.googleClientId.length > 0 ? (
    <GoogleOAuthProvider clientId={appConfig.googleClientId}>{app}</GoogleOAuthProvider>
  ) : (
    app
  ),
);