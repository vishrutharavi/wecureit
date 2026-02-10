"use client";

import styles from "../patient.module.scss";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import React from "react";

type Props = {
  patientName?: string;
};

export default function PatientHeader({ patientName }: Props) {
  // Use state + effect to load patient name from localStorage if not passed via props.
  // Initialize to a stable server-renderable default ('Patient') so server and
  // client markup match during hydration. The effect below will replace it
  // with a real name (or short UID) after mount if available.
  const [displayName, setDisplayName] = React.useState<string>(patientName ?? "Patient");

  React.useEffect(() => {
    if (patientName) {
      setDisplayName(patientName);
      return;
    }
    try {
      const raw = typeof window !== 'undefined' ? localStorage.getItem('patientProfile') : null;
      if (raw) {
        const p = JSON.parse(raw);
        const resolved = p?.name || p?.email || (p?.uid ? `Patient ${String(p.uid).slice(0, 6)}` : "Patient");
        // If the resolved value is an email, format the local-part for display
        const formatLocal = (val: string) => {
          if (!val) return val;
          if (val.includes('@')) {
            const local = val.split('@')[0];
            // Replace common separators with spaces and capitalize words
            return local
              .replace(/[._\-]+/g, ' ')
              .split(' ')
              .map((s) => s.charAt(0).toUpperCase() + s.slice(1))
              .join(' ');
          }
          return val;
        };

        setDisplayName(formatLocal(resolved));
      } else {
        setDisplayName("Patient");
      }
    } catch {
      setDisplayName("Patient");
    }
  }, [patientName]);
  const router = useRouter();
  const pathname = usePathname() || "";

  // Use reactive search params from Next.js so the active tab updates
  // immediately on client-side navigation (router.push). This avoids
  // manual popstate handling and keeps server/client markup stable.
  const searchParams = useSearchParams();
  const currentTab = React.useMemo(() => {
    try {
      return searchParams?.get('tab') || 'home';
    } catch { return 'home'; }
  }, [searchParams]);

  const isActive = (key: string) => {
    // key can be: 'home', 'profile', or a path like '/protected/patient/appointments'
    if (key === "home") return pathname === "/protected/patient" && (currentTab === "home" || !currentTab);
    if (key === "profile") return pathname === "/protected/patient" && currentTab === "profile";
    if (key === "appointments") return pathname === "/protected/patient" && currentTab === "appointments";
    if (!pathname) return false;
    return pathname.startsWith(key);
  };
  return (
    <div className={`${styles.header} ${styles.headerSpaceBetween}`}>
      <div className={styles.headerLeftColumn}>
        <h1 className={`${styles.title} ${styles.titleLeft}`}>
          Welcome, <span className={styles.welcomeDoctorName}>{displayName}</span>
        </h1>

        {/* CTAs aligned under the welcome text, left side */}
        <div className={styles.tabs}>
          <button
            className={styles.tab + (isActive("home") ? " " + styles.active : "")}
            onClick={() => router.push('/protected/patient?tab=home')}
          >
            Home
          </button>

          <button
            className={styles.tab + (isActive("profile") ? " " + styles.active : "")}
            onClick={() => router.push('/protected/patient?tab=profile')}
          >
            My Profile
          </button>

          <button
            className={styles.tab + (isActive("appointments") ? " " + styles.active : "")}
            onClick={() => router.push('/protected/patient?tab=appointments')}
          >
            Appointment History
          </button>
        </div>
      </div>

      <div className={styles.headerActions}>
        <button
          className={styles.viewAppointmentsBtn}
          onClick={() => {
            try {
              localStorage.removeItem('patientProfile');
            } catch {}
            router.push('/public/patient/login');
          }}
        >
          Sign out
        </button>
      </div>
    </div>
  );
}
