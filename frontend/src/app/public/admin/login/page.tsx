"use client";

import { useState } from "react";
import { login } from "@/lib/auth";
import { apiFetch } from "@/lib/api";
import type { User as FirebaseUser } from "firebase/auth";
import { getIdTokenResult } from "firebase/auth";
import styles from "./login.module.scss";
import { Lock, Eye, EyeOff } from "lucide-react";
import { useRouter } from "next/navigation";

export default function AdminLogin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const router = useRouter();

  async function handleLogin() {
    try {
      const user = await login(email, password);
      const token = await (user as FirebaseUser).getIdToken(true);

      // persist token for subsequent requests/UI
      if (typeof window !== "undefined") {
        localStorage.setItem("idToken", token);
      }

      // inspect claims (helps debug missing 'role' claim)
      try {
        const result = await getIdTokenResult(user as FirebaseUser);
        console.log("ID token claims:", result.claims);
      } catch (e) {
        console.warn("Could not read token result", e);
      }

      const me = await apiFetch("/api/admin/me", token);
      console.log(me);

      router.push("/protected/admin");
    } catch (err: unknown) {
      console.error("Admin login failed", err);
      const msg = err instanceof Error ? err.message : String(err);
      const lower = msg.toLowerCase();
      const friendly = (lower.includes('wrong') || lower.includes('password') || lower.includes('auth/')) ? 'Incorrect email or password' : msg;
      setToast(friendly);
      setTimeout(() => setToast(null), 4000);
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.card}>
        <Lock size={56} className={styles.icon} />

        <h2 className={styles.title}>Admin Portal</h2>
        <p className={styles.subtitle}>Login to access your dashboard</p>

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
