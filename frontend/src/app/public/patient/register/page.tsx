"use client";

import { useState } from "react";
import { apiFetch } from "@/lib/api";
import { Eye, EyeOff } from "lucide-react";
import styles from "./register.module.scss";

export default function PatientSignup() {
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    phone: "",
    dob: "",
    gender: "",
    city: "",
    state: "",
    zip: "",
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [step, setStep] = useState(1);


  function updateField(key: string, value: string) {
    setForm(prev => ({ ...prev, [key]: value }));
  }

  function getLocalDateYYYYMMDD() {
  const d = new Date();
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}


  async function handleSignup() {
  if (form.password !== form.confirmPassword) {
    alert("Passwords do not match");
    return;
  }

  await apiFetch("/api/auth/signup", undefined, {
    method: "POST",
    body: JSON.stringify({
      email: form.email,
      password: form.password,
      role: "PATIENT",
      name: form.name,
      phone: form.phone,
      dob: form.dob,
      gender: form.gender,
      city: form.city,
      state: form.state,
      zip: form.zip,
    }),
  });

  alert("Signup successful. Please login.");
  window.location.href = "/public/patient/login";
}



  return (
  <div className={styles.signupCard}>
    <h2>Create Account</h2>
    <p>Join us and start your healthcare journey</p>

    {step === 1 && (
      <>
        <input
          className={styles.input}
          placeholder="Full Name"
          value={form.name}
          required
          onChange={(e) => updateField("name", e.target.value)}
        />

        <input
          className={styles.input}
          placeholder="Email"
          value={form.email}
          required
          onChange={(e) => updateField("email", e.target.value)}
        />

        <input
          className={styles.input}
          placeholder="Phone Number"
          value={form.phone}
          required
          inputMode="numeric"
          pattern="[0-9]*"
          maxLength={10}
          onChange={(e) => {const digitsOnly = e.target.value.replace(/\D/g, "");
            if (digitsOnly.length <= 10) {
              updateField("phone", digitsOnly);
            }}}
        />

        <input
        className={styles.input}
        type="date"
        max={getLocalDateYYYYMMDD()}
        value={form.dob}
        required
        onChange={(e) => updateField("dob", e.target.value)}
        />


        <select
          className={styles.input}
          value={form.gender}
          required
          onChange={(e) => updateField("gender", e.target.value)}
        >
          <option value="">Select Gender</option>
          <option value="FEMALE">Female</option>
          <option value="MALE">Male</option>
          <option value="OTHER">Other</option>
        </select>

        <input
          className={styles.input}
          placeholder="City"
          value={form.city}
          required
          onChange={(e) => updateField("city", e.target.value)}
        />

        <input
          className={styles.input}
          placeholder="State"
          value={form.state}
          required
          onChange={(e) => updateField("state", e.target.value)}
        />

        <input
          className={styles.input}
          placeholder="ZIP Code"
          value={form.zip}
          required
          inputMode="numeric"
          pattern="[0-9]*"
          maxLength={5}
          onChange={(e) => {const digitsOnly = e.target.value.replace(/\D/g, "");
            if (digitsOnly.length <= 5) {
              updateField("zip", digitsOnly);
            }
          }}
        />

        <div className={styles.stepNav}>
          <button
            type="button"
            className={styles.primaryBtn}
            onClick={() => {
              if (!form.name || !form.email || !form.phone || !form.dob || !form.gender || !form.city || !form.state || !form.zip) {
                alert("Please fill in all required fields before continuing.");
                return;
              }
              setStep(2);
            }}
          >
            Next →
          </button>
        </div>
      </>
    )}

    {step === 2 && (
      <>
        <div className={styles.passwordField}>
          <input
            className={styles.input}
            type={showPassword ? "text" : "password"}
            placeholder="Create Password"
            value={form.password}
            required
            maxLength={8}
            onChange={(e) => updateField("password", e.target.value)}
          />

          <button
            type="button"
            className={styles.eyeButton}
            onClick={() => setShowPassword((v) => !v)}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>

        <div className={styles.passwordField}>
          <input
            className={styles.input}
            type={showConfirmPassword ? "text" : "password"}
            placeholder="Re-enter Password"
            value={form.confirmPassword}
            required
            maxLength={8}
            onChange={(e) => updateField("confirmPassword", e.target.value)}
          />

          <button
            type="button"
            className={styles.eyeButton}
            onClick={() => setShowConfirmPassword((v) => !v)}
          >
            {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>

        <div className={styles.stepNav}>
          <button className={styles.primaryBtn} onClick={handleSignup}>
            Register →
          </button>
        </div>

        <div className={styles.stepNav}>
          <button
            type="button"
            className={styles.backBtn} 
            onClick={() => setStep(1)}
          >
            ← Back
          </button>
        </div>
      </>
    )}
  </div>
  )
}