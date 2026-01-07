"use client";

import { useState } from "react";
import { signup } from "@/lib/auth";
import { apiFetch } from "@/lib/api";

export default function PatientSignup() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  async function handleSignup() {
    const token = await signup(email, password);

    await apiFetch("/api/auth/patient/signup", token, {
      method: "POST",
      body: JSON.stringify({}),
    });

    alert("Signup successful");
  }

  return (
    <div>
      <input placeholder="email" onChange={e => setEmail(e.target.value)} />
      <input placeholder="password" type="password" onChange={e => setPassword(e.target.value)} />
      <button onClick={handleSignup}>Register</button>
    </div>
  );
}
