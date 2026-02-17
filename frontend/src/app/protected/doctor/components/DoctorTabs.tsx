"use client";

import { useState, useEffect, useCallback } from "react";
import styles from "../doctor.module.scss";
import { Calendar, Clock, FileText, Share2 } from "lucide-react";
import ReferralModal from "./Referral/ReferralModal";
import { getIncomingReferrals } from "@/lib/doctor/doctorApi";

type Tab = "schedule" | "availability" | "notes";

type Props = {
  active: Tab;
  onChange: (tab: Tab) => void;
};

export default function DoctorTabs({ active, onChange }: Props) {
  const [showReferrals, setShowReferrals] = useState(false);
  const [referralCount, setReferralCount] = useState(0);

  const getDoctorId = useCallback(() => {
    try {
      const raw = localStorage.getItem("doctorProfile");
      if (raw) return JSON.parse(raw).id;
    } catch {}
    return null;
  }, []);

  useEffect(() => {
    let mounted = true;
    async function fetchCount() {
      const doctorId = getDoctorId();
      if (!doctorId) return;
      try {
        const incoming = await getIncomingReferrals(doctorId);
        if (mounted && Array.isArray(incoming)) {
          const pending = incoming.filter((r: { status: string }) => r.status === "PENDING");
          setReferralCount(pending.length);
        }
      } catch {
        // ignore
      }
    }
    fetchCount();
    return () => { mounted = false; };
  }, [getDoctorId, showReferrals]);

  return (
    <div className={styles.tabs}>
      <div style={{ display: 'flex', gap: 8 }}>
        <button
          className={`${styles.tab} ${active === "schedule" ? styles.active : ""}`}
          onClick={() => onChange("schedule")}
        >
          <Calendar size={16} /> My Schedule
        </button>

        <button
          className={`${styles.tab} ${active === "availability" ? styles.active : ""}`}
          onClick={() => onChange("availability")}
        >
          <Clock size={16} /> Set Availability
        </button>

        <button
          className={`${styles.tab} ${active === "notes" ? styles.active : ""}`}
          onClick={() => onChange("notes")}
        >
          <FileText size={16} /> Appointments & Notes
        </button>
      </div>

      <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button className={`${styles.tab} ${showReferrals ? styles.active : ''}`} onClick={() => setShowReferrals(true)} title="View Referrals">
          <Share2 size={14} style={{ marginRight: 8 }} /> Referrals {referralCount > 0 ? <span style={{ marginLeft: 6 }} className={styles.smallBadge}>{referralCount}</span> : null}
        </button>
      </div>

      <ReferralModal open={showReferrals} onClose={() => setShowReferrals(false)} />
    </div>
  );
}
