"use client";

import { useState } from "react";
import { login } from "@/lib/auth";
import { apiFetch } from "@/lib/api";
import type { User as FirebaseUser } from "firebase/auth";
import { getIdTokenResult } from "firebase/auth";
import styles from "./login.module.scss";
import { Lock } from "lucide-react";
import { useRouter } from "next/navigation";

export default function AdminLogin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
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
      alert("Admin login failed: " + msg);
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
