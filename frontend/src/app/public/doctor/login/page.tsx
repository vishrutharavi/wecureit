"use client";

import { useState } from "react";
import { login } from "@/lib/auth";
import { apiFetch, showInlineToast } from "@/lib/api";
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
      // Force-refresh token to ensure we send a fresh ID token for linking
      const idToken = await (user as FirebaseUser).getIdToken(true);

      // Try to link this Firebase identity to a doctor record so server can set role claim.
      try {
        await apiFetch('/api/auth/link', idToken, { method: 'POST', body: JSON.stringify({ portal: 'DOCTOR' }) });
        // after linking, force-refresh token to pick up custom claims (best-effort)
        try {
          const fresh = await (user as FirebaseUser).getIdToken(true);
          try { localStorage.setItem('doctorToken', fresh); } catch {}
          try { localStorage.setItem('idToken', fresh); } catch {}
        } catch (e) {
          // non-fatal if refresh fails
          console.warn('Failed to refresh token after linking', e);
        }
      } catch (linkErr) {
        // linking is best-effort; log and continue to fetch profile (may 403 if not linked)
        console.warn('Linking Firebase identity to doctor failed', linkErr);
      }

      // Fetch profile; retry briefly if backend hasn't yet associated the firebase UID with a DB doctor record.
      let me = await apiFetch("/api/doctor/me");
      console.log('doctor me', me);
      if (!me || !me.id) {
        // try up to 3 times with a forced token refresh between attempts (best-effort to wait for backend claim/write)
        for (let i = 0; i < 2 && (!me || !me.id); i++) {
          try {
            const fresh = await (user as FirebaseUser).getIdToken(true);
            try { localStorage.setItem('doctorToken', fresh); } catch {}
            try { localStorage.setItem('idToken', fresh); } catch {}
          } catch {}
          try { me = await apiFetch('/api/doctor/me'); } catch (e) { console.warn('Retrying /api/doctor/me failed', e); }
        }
      }

      // persist a lightweight doctor profile for client usage
      try {
        if (me && me.id) {
          localStorage.setItem('doctorProfile', JSON.stringify(me));
          // persist id token so subsequent API calls can include it
          try { localStorage.setItem('doctorToken', idToken); } catch {}
          try { localStorage.setItem('idToken', idToken); } catch {}
        } else {
          // If we still don't have an id, inform the user instead of navigating into a broken dashboard.
          try { showInlineToast('Account not linked to a doctor record. Please contact support or try linking again.'); } catch {}
          return;
        }
      } catch (e) {
        console.warn('Failed to persist doctor profile or tokens', e);
      }
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