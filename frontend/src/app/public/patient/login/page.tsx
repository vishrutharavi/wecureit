"use client";

import { useState } from "react";
import { login } from "@/lib/auth";
import { apiFetch } from "@/lib/api";
import styles from "./login.module.scss";
import { User, Eye, EyeOff } from "lucide-react";
import { useRouter } from "next/navigation";
import type { User as FirebaseUser } from "firebase/auth";


export default function PatientLogin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  async function handleLogin() {
    try {
      // login() returns a Firebase User object; retrieve an ID token string
      const user = await login(email, password);
      const idToken = await (user as FirebaseUser).getIdToken();

      const me = await apiFetch("/api/patient/me", idToken);
      console.log(me);

      // persist a lightweight patient profile for client usage similar to doctor flow
      try {
        if (me) {
          // Prefer a human-friendly name from the API, but fall back to
          // Firebase user displayName or email if the API returns only minimal data.
          const firebaseUser = user as FirebaseUser;
          const profile = {
            ...me,
            name: me?.name || firebaseUser?.displayName || firebaseUser?.email || undefined,
          };
          localStorage.setItem('patientProfile', JSON.stringify(profile));
          // persist id token for subsequent API calls
          try { localStorage.setItem('patientToken', idToken); } catch {}
        }
      } catch {}
      try {
        sessionStorage.setItem(
          'patientJustLoggedIn',
          JSON.stringify({ name: me && (me.name || me.email) ? (me.name ?? me.email) : 'Patient', time: Date.now() })
        );
      } catch {}

      // navigate to protected patient area
      window.location.href = "/protected/patient";
    } catch (err: unknown) {
      console.error("Login failed", err);
      const msg = err instanceof Error ? err.message : String(err);
      const lower = msg.toLowerCase();
      const friendly = (lower.includes('wrong') || lower.includes('password') || lower.includes('auth/')) ? 'Incorrect email or password' : msg;
      setToast(friendly);
      setTimeout(() => setToast(null), 4000);
    }
  }


  const router = useRouter();

  return (
    <div className={styles.wrapper}>
      <div className={styles.card}>
        <User size={56} className={styles.icon} />

        <h2 className={styles.title}>Patient Portal</h2>
        <p className={styles.subtitle}>Login to access your account</p>

        <input
          className={styles.input}
          placeholder="Email"
          onChange={(e) => setEmail(e.target.value)}
        />

        <div className={styles.passwordWrap}>
          <input
            className={styles.input}
            type={showPassword ? "text" : "password"}
            placeholder="Password"
            onChange={(e) => setPassword(e.target.value)}
          />
          <button
            type="button"
            className={styles.eyeBtn}
            onClick={() => setShowPassword((s) => !s)}
            aria-label={showPassword ? "Hide password" : "Show password"}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>

        <button className={styles.viewAppointmentsBtn} onClick={handleLogin}>
          Login
        </button>

        <button className={styles.backBtn} onClick={() => router.push("/")}>
            ← Back to Home
        </button>

        {toast && <div className={styles.toast}>{toast}</div>}

      </div>
    </div>
  );
}
