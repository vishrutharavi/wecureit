"use client";

import styles from "../doctor.module.scss";
import { useRouter } from "next/navigation";

import React from "react";

type Props = {
  doctorName?: string;
};

export default function DoctorHeader({
  doctorName = "Dr. Sarah Mitchell",
}: Props) {
  const raw = doctorName || "";
  // remove any existing Dr. prefix (with or without a period) then re-prefix once
  const normalized = raw.replace(/^Dr\.?\s*/i, "");
  const displayName = `Dr. ${normalized}`.trim();
  const router = useRouter();

  return (
    <div className={styles.header}>
      <div>
        <h1 className={styles.title}>Welcome, <span className={styles.welcomeDoctorName}>{displayName}</span></h1>
      </div>
      <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 8 }}>
        <button
          className={styles.viewAppointmentsBtn}
          onClick={() => {
            try {
              // clear doctor profile and any session-like keys
              localStorage.removeItem('doctorProfile');
            } catch {}
            // redirect to public doctor login
            router.push('/public/doctor/login');
          }}
        >
          Sign out
        </button>
      </div>
    </div>
  );
}
