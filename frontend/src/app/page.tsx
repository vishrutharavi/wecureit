"use client";

import { useRouter } from "next/navigation";
import styles from "./app.module.scss";
import { User, UserPlus, Stethoscope, Lock } from "lucide-react";

export default function HomePage() {
  const router = useRouter();

  return (
    <div className={styles.wrapper}>
      {/* HERO */}
      <main className={styles.hero}>
        <h1 className={styles.title}>Welcome to WeCureIT</h1>
        <p className={styles.subtitle}>
          Medical appointment booking made easy
        </p>

        {/* HIPAA Banner */}
        <div className={styles.hipaaBanner}>
          THIS SOFTWARE IS NOT HIPAA COMPLIANT OR ANY OTHER COMPLIANT
        </div>

        {/* Patient Card */}
        <div className={styles.card}>
          <User size={56} className={styles.icon} />
          <h2>Patient Portal</h2>
          <p>Book visits and manage your records easily</p>

          <button
            className={styles.viewAppointmentsBtn}
            onClick={() => router.push("/public/patient/login")}
          >
            Login as Patient →
          </button>
        </div>

        {/* Register */}
        <div className={styles.registerBox}>
          <UserPlus size={32} className={styles.icon} />
          <div>
            <h4>New Patient?</h4>
            <p>Create an account to get started</p>
          </div>
          <button
            className={styles.secondaryBtn}
            onClick={() => router.push("/public/patient/register")}
          >
            Register Now
          </button>
        </div>
      </main>

      {/* Floating Doctor Button */}
      <button
        className={styles.fabDoctor}
        title="Doctor Login"
        onClick={() => router.push("/public/doctor/login")}
      >
        <Stethoscope />
      </button>

      {/* Floating Admin Button */}
      <button
        className={styles.fabAdmin}
        title="Admin Login"
        onClick={() => router.push("/public/admin/login")}
      >
        <Lock />
      </button>

      {/* Footer moved to RootLayout (persistent footer for all pages) */}
    </div>
  );
}
