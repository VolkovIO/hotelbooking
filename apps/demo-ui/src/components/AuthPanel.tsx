import { GoogleLogin, googleLogout } from "@react-oauth/google";
import { useAuth } from "../auth/AuthContext";
import { appConfig } from "../config/appConfig";

export function AuthPanel() {
  const {
    authMode,
    userEmail,
    userName,
    userPictureUrl,
    isAuthenticated,
    signInWithGoogleCredential,
    signOut,
  } = useAuth();

  function handleSignOut() {
    googleLogout();
    signOut();
  }

  if (authMode === "demo") {
    return (
      <div className="auth-card">
        <span>Auth</span>
        <strong>demo</strong>
        <small>{userEmail}</small>
      </div>
    );
  }

  return (
    <div className="auth-card auth-card-google">
      <span>Google auth</span>

      {isAuthenticated ? (
        <>
          <div className="google-user">
            {userPictureUrl !== null && <img alt="" src={userPictureUrl} />}

            <div className="google-user-text">
              <strong>{userName ?? "Google user"}</strong>
              <small>{userEmail}</small>
            </div>
          </div>

          <button className="auth-signout-button" type="button" onClick={handleSignOut}>
            Sign out
          </button>
        </>
      ) : appConfig.googleClientId.length === 0 ? (
        <small>Set VITE_GOOGLE_CLIENT_ID in .env.local</small>
      ) : (
        <div className="google-login-standard-wrapper">
          <GoogleLogin
            type="standard"
            theme="outline"
            size="large"
            shape="rectangular"
            text="signin_with"
            logo_alignment="left"
            locale="en"
            width="240"
            onSuccess={signInWithGoogleCredential}
            onError={() => {
              window.alert("Google sign-in failed");
            }}
          />
        </div>
      )}
    </div>
  );
}