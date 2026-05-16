import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import type { CredentialResponse } from "@react-oauth/google";
import { appConfig } from "../config/appConfig";

type GoogleJwtPayload = {
  email?: string;
  name?: string;
  picture?: string;
  exp?: number;
};

type AuthContextValue = {
  authMode: "demo" | "google";
  authToken: string | null;
  userEmail: string;
  userName: string | null;
  userPictureUrl: string | null;
  isAuthenticated: boolean;
  canCallBookingApi: boolean;
  signInWithGoogleCredential: (credentialResponse: CredentialResponse) => void;
  signOut: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

type AuthProviderProps = {
  children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [authToken, setAuthToken] = useState<string | null>(null);
  const [googleProfile, setGoogleProfile] = useState<GoogleJwtPayload | null>(null);

  const value = useMemo<AuthContextValue>(() => {
    const isGoogleAuthenticated = appConfig.authMode === "google" && authToken !== null;

    return {
      authMode: appConfig.authMode,
      authToken,
      userEmail:
        appConfig.authMode === "demo"
          ? appConfig.demoUserEmail
          : googleProfile?.email ?? "Not signed in",
      userName: googleProfile?.name ?? null,
      userPictureUrl: googleProfile?.picture ?? null,
      isAuthenticated: appConfig.authMode === "demo" || isGoogleAuthenticated,
      canCallBookingApi: appConfig.authMode === "demo" || isGoogleAuthenticated,
      signInWithGoogleCredential,
      signOut,
    };
  }, [authToken, googleProfile]);

  function signInWithGoogleCredential(credentialResponse: CredentialResponse) {
    if (!credentialResponse.credential) {
      throw new Error("Google credential is empty");
    }

    setAuthToken(credentialResponse.credential);
    setGoogleProfile(decodeJwtPayload(credentialResponse.credential));
  }

  function signOut() {
    setAuthToken(null);
    setGoogleProfile(null);
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (context === null) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}

function decodeJwtPayload(token: string): GoogleJwtPayload | null {
  const [, payload] = token.split(".");

  if (!payload) {
    return null;
  }

  try {
    const normalizedPayload = payload.replace(/-/g, "+").replace(/_/g, "/");
    const decoded = window.atob(normalizedPayload);
    const json = decodeURIComponent(
      Array.from(decoded)
        .map((char) => `%${char.charCodeAt(0).toString(16).padStart(2, "0")}`)
        .join(""),
    );

    return JSON.parse(json) as GoogleJwtPayload;
  } catch {
    return null;
  }
}