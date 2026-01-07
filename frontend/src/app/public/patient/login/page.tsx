"use client";

import { useState } from "react";
import { login } from "@/lib/auth";
import { apiFetch } from "@/lib/api";

export default function PatientLogin() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function handleLogin() {
    const token = await login(email, password);

    const me = await apiFetch("/api/patient/me", token);
    console.log(me);

    alert("Logged in");
  }

  return (
    <div>
      <input placeholder="email" onChange={e => setEmail(e.target.value)} />
      <input placeholder="password" type="password" onChange={e => setPassword(e.target.value)} />
      <button onClick={handleLogin}>Login</button>
    </div>
  );
}
