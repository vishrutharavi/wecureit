"use client";

import { useState } from "react";
import { login } from "@/lib/auth";
import { apiFetch } from "@/lib/api";
import styles from "./login.module.scss";
import { Stethoscope, Eye, EyeOff } from "lucide-react";
import { useRouter } from "next/navigation";
import type { User as FirebaseUser } from "firebase/auth";


export default function DoctorLogin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [toast, setToast] = useState<string | null>(null);
  const router = useRouter();

  async function handleLogin() {
    try {
      const user = await login(email, password);
      const idToken = await (user as FirebaseUser).getIdToken();

      const me = await apiFetch("/api/doctor/me", idToken);
      console.log('doctor me', me);

      // persist a lightweight doctor profile for client usage
      try {
        if (me) localStorage.setItem('doctorProfile', JSON.stringify(me));
        // persist id token so subsequent API calls can include it
        try { localStorage.setItem('doctorToken', idToken); } catch {}
      } catch {}
      // mark that we just logged in so the dashboard can show a non-blocking message
      try {
        sessionStorage.setItem(
          'doctorJustLoggedIn',
          JSON.stringify({ name: me && (me.name || me.email) ? (me.name ?? me.email) : 'Doctor', time: Date.now() })
        );
      } catch {}

      // navigate to doctor dashboard (protected route)
      router.push('/protected/doctor');
    } catch (err: unknown) {
      console.error("Login failed", err);
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
        <Stethoscope size={56} className={styles.icon} />

        <h2 className={styles.title}>Doctor Portal</h2>
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