"use client";

export const dynamic = "force-dynamic";

import React, { useEffect, useState, Suspense } from "react";
import type { Doctor, Facility, Specialty } from '@/app/protected/patient/types';
import { useRouter, useSearchParams } from "next/navigation";

// Isolated component so useSearchParams is inside a Suspense boundary
function TabSyncer({ onTab }: { onTab: (t: string) => void }) {
  const searchParams = useSearchParams();
  useEffect(() => {
    const t = searchParams?.get("tab") || "home";
    onTab(t);
  }, [searchParams, onTab]);
  return null;
}
import styles from "./patient.module.scss";
import PatientHeader from "./components/PatientHeader";
import Home from "./components/Home/Home";
import PersonalInfo from "./components/MyProfile/PersonalInfo";
import Address from "./components/MyProfile/Address";
import Payment from "./components/MyProfile/Payment";
import AppointmentHistory from "./components/AppointmentHistory/AppointmentHistory";
import PatientReferrals from "./components/Referrals/PatientReferrals";
import DropdownSelection from "./components/DropdownSelection/DropdownSelection";
import SelectionSummary from "./components/DropdownSelection/SelectionSummary";
import DateAndTimeSelection from "./components/DateAndTimeSelection/DateAndTimeSelection";
import Confirmation from "./components/AppointmentSummary/Confirmation";
import BookingCopilot from "./components/BookingCopilot/BookingCopilot";
//import BookingCopilot from "./components/BookingCopilot/BookingCopilot";

export default function Page() {
  const router = useRouter();
  const [tab, setTab] = useState<string>("home");
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
      <div className={styles.content}>
        <Suspense fallback={null}>
          <TabSyncer onTab={setTab} />
        </Suspense>
        {justLoggedInMsg && <div className={styles.loginBanner}>{justLoggedInMsg}</div>}
        <Suspense fallback={null}>
          <PatientHeader />
        </Suspense>

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
        ) : tab === "referrals" ? (
          <>
            <PatientReferrals />
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
    </div>
  );
}

// A small client-side booking component kept in this file for convenience.
// It can be imported elsewhere if you prefer a separate page file.
export function BookAppointmentPage() {
  type Selection = { doctor?: Doctor | null; facility?: Facility | null; specialty?: Specialty | null; duration?: number | null };

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

      <BookingCopilot />
      <div className={styles.manualDivider}>
        <span>or book manually with the dropdowns below</span>
      </div>

      <div className={styles.bookingGrid}>
        <div>
          <DropdownSelection onChange={handleSelectionChange} />
        </div>

        <div>
          <SelectionSummary doctor={selection.doctor} facility={selection.facility} specialty={selection.specialty} duration={selection.duration} />
        </div>
      </div>
    </div>
  );
}
