"use client";

import { useState } from "react";
import { login } from "@/lib/auth";
import { apiFetch } from "@/lib/api";
import styles from "./login.module.scss";
import { Stethoscope } from "lucide-react";
import { useRouter } from "next/navigation";
import type { User as FirebaseUser } from "firebase/auth";


export default function DoctorLogin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
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
      alert("Login failed: " + msg);
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

        <input
          className={styles.input}
          type="password"
          placeholder="Password"
          onChange={(e) => setPassword(e.target.value)}
        />

        <button className={styles.primaryBtn} onClick={handleLogin}>
          Login
        </button>

        <button className={styles.backBtn} onClick={() => router.push("/")}>
            ← Back to Home
        </button>

      </div>
    </div>
  );
}