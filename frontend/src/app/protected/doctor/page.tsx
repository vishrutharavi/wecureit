"use client";

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import styles from "./doctor.module.scss";
import DoctorHeader from "./components/DoctorHeader";
import DoctorTabs from "./components/DoctorTabs";
import ScheduleView from "./components/Schedule/ScheduleView";
import AvailabilityView from "./components/Availability/AvailabilityView";
import NotesView from "./components/Notes/NotesViewGrid";


export default function DoctorPage() {
  const searchParams = useSearchParams();
  const initialTab = (searchParams?.get("tab") as "schedule" | "availability" | "notes") || "schedule";
  const router = useRouter();
  const [tab, setTab] = useState<"schedule" | "availability" | "notes">(initialTab);
  const handleTabChange = (t: "schedule" | "availability" | "notes") => {
    setTab(t);
    // update URL so it reflects current tab
    try {
      router.push(`/protected/doctor?tab=${t}`);
    } catch {
      // fallback: no-op if router isn't available
    }
  };
  const [doctorName, setDoctorName] = useState<string | undefined>(undefined);
  const [justLoggedInMsg, setJustLoggedInMsg] = useState<string | null>(null);

  useEffect(() => {
    try {
      const raw = localStorage.getItem('doctorProfile');
      if (raw) {
        const obj = JSON.parse(raw);
        // defer to avoid synchronous setState in effect
        setTimeout(() => setDoctorName(obj.name ?? obj.email ?? undefined), 0);
      } else {
        // not logged in as doctor -> redirect to doctor login
        try {
          router.push('/public/doctor/login');
        } catch {}
      }
    } catch {}
  }, [router]);

  useEffect(() => {
    try {
      const s = sessionStorage.getItem('doctorJustLoggedIn');
      if (s) {
        const obj = JSON.parse(s);
        // defer state update to avoid synchronous setState in effect
        setTimeout(() => {
          setJustLoggedInMsg(`Logged in as ${obj.name}`);
          try { sessionStorage.removeItem('doctorJustLoggedIn'); } catch {}
          setTimeout(() => setJustLoggedInMsg(null), 3000);
        }, 0);
      }
    } catch {}
  }, []);

  return (
    <div className={styles.wrapper}>
      {justLoggedInMsg && <div className={styles.loginBanner}>{justLoggedInMsg}</div>}
      <DoctorHeader doctorName={doctorName} />
      <DoctorTabs active={tab} onChange={handleTabChange} />

      {tab === "schedule" && <ScheduleView />}
      {tab === "availability" && <AvailabilityView />}
      {tab === "notes" && <NotesView />}
    </div>
  );
}
