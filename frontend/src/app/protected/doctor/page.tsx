"use client";

import { useState, useEffect } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { ArrowLeft, MapPin, Calendar } from "lucide-react";
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

// A small referral page component kept in this file per request. Exported as a named
// component so it can be imported/used from other places if needed.
export function ReferPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const patient = searchParams?.get("patient") || "Patient";

  return (
    <div style={{ padding: "1rem 0" }}>
      <a
        style={{ display: "inline-flex", alignItems: "center", gap: 8, color: "var(--doctor-primary)", cursor: "pointer" }}
        onClick={() => router.push("/protected/doctor?tab=notes")}
      >
        <ArrowLeft size={16} /> Back to Appointments
      </a>

      <div className={styles.pageInner}>
        <div className={styles.scheduleContainer} style={{ marginTop: 16 }}>
          <h4 style={{ margin: 0, display: "flex", alignItems: "center", gap: 8 }}>
            <MapPin size={16} /> Create Referral
          </h4>
          <p style={{ color: "#666", marginTop: 6 }}>Refer {patient} to a specialist</p>

          <div className={`${styles.compactCard} ${styles.referCard}`} style={{ marginTop: 12 }}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12 }}>
              <div>
                <div style={{ color: "#666", fontSize: 13 }}>Patient</div>
                <div style={{ fontWeight: 800, marginTop: 6 }}>{patient}</div>
                <div style={{ color: "#666", marginTop: 6 }}>47 years old • Female</div>

                <div style={{ marginTop: 12 }}>
                  <div style={{ color: "#666", fontSize: 13 }}>Chief Complaint</div>
                  <div style={{ marginTop: 8 }}>Post-surgery follow-up</div>
                </div>
              </div>

              <div style={{ minWidth: 220 }}>
                <div style={{ color: "#666", fontSize: 13 }}>Original Appointment</div>
                <div style={{ marginTop: 6 }}>
                  <Calendar size={14} /> <span style={{ marginLeft: 8 }}>January 16, 2026</span>
                </div>
                <div style={{ marginTop: 6 }}>
                  <MapPin size={14} /> <span style={{ marginLeft: 8 }}>Downtown Medical Center</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className={styles.pageInner}>
        <div className={styles.scheduleContainer} style={{ marginTop: 18 }}>
          <h4 style={{ marginTop: 0 }}>Select Specialty and State</h4>
          <p style={{ color: "#666", marginTop: 6 }}>Choose the specialty and state to view available doctors for referral</p>

          <div style={{ display: "flex", gap: 12, marginTop: 12 }}>
            <div style={{ flex: 1 }}>
              <label style={{ display: "block", fontSize: 13, marginBottom: 6 }}>Specialty *</label>
              <select style={{ width: "100%", padding: "12px", borderRadius: 8, border: "1px solid rgba(239,68,68,0.12)" }}>
                <option value="">Select specialty...</option>
              </select>
            </div>

            <div style={{ width: 260 }}>
              <label style={{ display: "block", fontSize: 13, marginBottom: 6 }}>State *</label>
              <select style={{ width: "100%", padding: "12px", borderRadius: 8, border: "1px solid rgba(239,68,68,0.12)" }}>
                <option value="">Select state...</option>
              </select>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
