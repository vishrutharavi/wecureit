"use client";

import React, { useEffect, useState } from "react";
import type { Doctor, Facility, Specialty } from '@/app/protected/patient/types';
import { useRouter, useSearchParams } from "next/navigation";
import styles from "./patient.module.scss";
import PatientHeader from "./components/PatientHeader";
import Home from "./components/Home/Home";
import PersonalInfo from "./components/MyProfile/PersonalInfo";
import Address from "./components/MyProfile/Address";
import Payment from "./components/MyProfile/Payment";
import AppointmentHistory from "./components/AppointmentHistory/AppointmentHistory";
import DropdownSelection from "./components/DropdownSelection/DropdownSelection";
import SelectionSummary from "./components/DropdownSelection/SelectionSummary";
import DateAndTimeSelection from "./components/DateAndTimeSelection/DateAndTimeSelection";
import Confirmation from "./components/AppointmentSummary/Confirmation";

export default function Page() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [tab, setTab] = useState<string>("home");

  // Keep the tab state in sync with the URL's `tab` search param. Using
  // `useSearchParams()` inside a client component makes this reactive to
  // client-side navigation (router.push) without manual popstate handling.
  useEffect(() => {
    try {
      const t = searchParams?.get('tab') || 'home';
      // Defer setState to avoid a synchronous state update inside the effect
      // which can cause cascading renders in some React setups.
      setTimeout(() => setTab(t), 0);
    } catch {}
  }, [searchParams]);
  const [justLoggedInMsg, setJustLoggedInMsg] = useState<string | null>(null);

  useEffect(() => {
    try {
      const raw = localStorage.getItem('patientProfile');
      if (raw) {
        // ok
      } else {
        // not logged in as patient -> redirect to patient login
        try {
          router.push('/public/patient/login');
        } catch {}
      }
    } catch {}
  }, [router]);

  useEffect(() => {
    try {
      const s = sessionStorage.getItem('patientJustLoggedIn');
      if (s) {
        const obj = JSON.parse(s);
        setTimeout(() => {
          setJustLoggedInMsg(`Logged in as ${obj.name}`);
          try { sessionStorage.removeItem('patientJustLoggedIn'); } catch {}
          setTimeout(() => setJustLoggedInMsg(null), 3000);
        }, 0);
      }
    } catch {}
  }, []);

  return (
    <div className={styles.wrapper} style={{ padding: 24 }}>
  {justLoggedInMsg && <div className={styles.loginBanner}>{justLoggedInMsg}</div>}
  <PatientHeader />

      <div style={{ paddingLeft: 16, paddingRight: 16 }}>
        {tab === "profile" ? (
          <>
            <div className={styles.profileHeader}>
              <div className={styles.profileHeaderLeft}>
                <div className={styles.profileTitle}>My Profile</div>
                <div className={styles.profileSubtitle}>Manage your personal information</div>
              </div>

              <div>{/* Page-level controls may be wired here if desired */}</div>
            </div>

            <div style={{ marginTop: 16, display: "grid", gap: 12 }}>
              <PersonalInfo />

              <Address />

              <Payment />
            </div>
          </>
        ) : tab === "dropdownselection" ? (
          <>
            <BookAppointmentPage />
          </>
        ) : tab === "datetimeselection" ? (
          <>
            <DateAndTimeSelection />
          </>
        ) : tab === "confirmation" ? (
          <>
            <Confirmation />
          </>
        ) : tab === "appointments" ? (
          <>
            <AppointmentHistory />
          </>
        ) : (
          <>
            <p className={styles.subtitle}>Access your records and appointments</p>

            {/* Render the patient home content (book card + upcoming appointments) */}
            <div style={{ marginTop: 28 }}>
              <Home />
            </div>
          </>
        )}
      </div>
    </div>
  );
}

// A small client-side booking component kept in this file for convenience.
// It can be imported elsewhere if you prefer a separate page file.
export function BookAppointmentPage() {
  type Selection = { doctor?: Doctor | null; facility?: Facility | null; specialty?: Specialty | null };

  const [selection, setSelection] = React.useState<Selection>({});

  const handleSelectionChange = React.useCallback((s: Selection) => {
    setSelection(s);
  }, []);

  return (
    <div className={styles.wrapper}>
      <div className={styles.bookingHeader}>
        <a href="/protected/patient" className={styles.backLink}>← Back to Home</a>
        <h1 className={styles.bookingTitle}>Book an Appointment</h1>
        <div className={styles.subtitle}>Select your doctor, facility, and specialty to continue</div>
      </div>

      <div className={styles.bookingGrid}>
        <div>
          <DropdownSelection onChange={handleSelectionChange} />
        </div>

        <div>
          <SelectionSummary doctor={selection.doctor} facility={selection.facility} specialty={selection.specialty} />
        </div>
      </div>
    </div>
  );
}
