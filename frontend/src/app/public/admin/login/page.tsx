"use client";

import { useState } from "react";
import { login } from "@/lib/auth";
import { apiFetch } from "@/lib/api";
import styles from "./login.module.scss";
import { Lock } from "lucide-react";
import { useRouter } from "next/navigation";


export default function DoctorLogin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function handleLogin() {
    const token = await login(email, password);

    const me = await apiFetch("/api/doctor/me", token);
    console.log(me);

    alert("Logged in");
  }

  const router = useRouter();
  


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