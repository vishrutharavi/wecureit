"use client";

import React, { useEffect, useState } from "react";
import styles from "@/app/protected/patient/patient.module.scss";
import { getPatientReferrals } from "@/lib/patient/patientApi";

type Referral = {
  id: string;
  fromDoctorName: string;
  fromDoctorEmail: string;
  toDoctorName: string;
  toDoctorEmail: string;
  specialityName: string;
  specialityCode: string;
  reason: string;
  status: string;
  cancelReason?: string;
  createdAt: string;
};

const STATUS_COLOR: Record<string, string> = {
  PENDING:   "#f97316",
  ACCEPTED:  "#3b82f6",
  COMPLETED: "#22c55e",
  CANCELLED: "#9ca3af",
};

export default function PatientReferrals() {
  const [referrals, setReferrals] = useState<Referral[]>([]);
  const [loading, setLoading]     = useState(true);
  const [filter, setFilter]       = useState<"ALL" | "PENDING" | "ACCEPTED" | "COMPLETED" | "CANCELLED">("ALL");

  useEffect(() => {
    async function load() {
      try {
        const raw = typeof window !== "undefined" ? localStorage.getItem("patientProfile") : null;
        if (!raw) return;
        const profile = JSON.parse(raw);
        const patientId = profile?.id;
        if (!patientId) return;
        const data = await getPatientReferrals(patientId);
        if (Array.isArray(data)) setReferrals(data as Referral[]);
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    }
    load();
  }, []);

  const filtered = filter === "ALL"
    ? referrals
    : referrals.filter((r) => r.status === filter);

  const statuses = ["ALL", "PENDING", "ACCEPTED", "COMPLETED", "CANCELLED"] as const;

  return (
    <div>
      <div className={styles.ahHeader}>
        <h2 className={styles.profileTitle}>My Referrals</h2>
        <div className={styles.subtitle}>Specialist referrals from your doctors</div>
      </div>

      <div className={`${styles.panelWhite} ${styles.panelTopSpacing}`}>
        <div className={styles.filtersRow}>
          {statuses.map((s) => (
            <button
              key={s}
              onClick={() => setFilter(s)}
              className={filter === s ? styles.editProfileBtn : `${styles.filterPill} ${styles.filterInactive}`}
            >
              {s === "ALL" ? "All" : s.charAt(0) + s.slice(1).toLowerCase()}
            </button>
          ))}
        </div>
      </div>

      <div className={styles.listWrap}>
        {loading ? (
          <div style={{ textAlign: "center", padding: "2rem", color: "#9ca3af" }}>Loading referrals…</div>
        ) : filtered.length === 0 ? (
          <div style={{ textAlign: "center", padding: "2rem", color: "#9ca3af" }}>
            {referrals.length === 0 ? "No referrals yet." : "No referrals match this filter."}
          </div>
        ) : (
          filtered.map((r) => (
            <div key={r.id} className={styles.appointmentCard}>
              <div className={styles.appointmentHeader}>
                <div>
                  <div className={styles.doctorRow}>
                    <h3>{r.toDoctorName}</h3>
                    <div className={styles.smallBadge}>{r.specialityName}</div>
                  </div>
                  <div className={styles.metaRow}>
                    <div className={styles.metaItem}>
                      <span style={{ color: "#6b7280", fontSize: "0.85rem" }}>Referred by {r.fromDoctorName}</span>
                    </div>
                    <div className={styles.metaItem}>
                      <span style={{ color: "#9ca3af", fontSize: "0.8rem" }}>
                        {r.createdAt ? new Date(r.createdAt).toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" }) : ""}
                      </span>
                    </div>
                  </div>
                  {r.reason && (
                    <div style={{ marginTop: "0.5rem", fontSize: "0.875rem", color: "#374151", fontStyle: "italic" }}>
                      &ldquo;{r.reason}&rdquo;
                    </div>
                  )}
                  {r.cancelReason && (
                    <div style={{ marginTop: "0.4rem", fontSize: "0.8rem", color: "#9ca3af" }}>
                      Cancelled: {r.cancelReason}
                    </div>
                  )}
                </div>
                <div>
                  <span style={{
                    background: (STATUS_COLOR[r.status] ?? "#9ca3af") + "20",
                    color: STATUS_COLOR[r.status] ?? "#9ca3af",
                    borderRadius: 999, padding: "0.3rem 0.75rem",
                    fontSize: "0.8rem", fontWeight: 700, whiteSpace: "nowrap",
                  }}>
                    {r.status}
                  </span>
                </div>
              </div>
              <hr className={styles.divider} />
              <div style={{ fontSize: "0.8rem", color: "#9ca3af" }}>
                Specialist: <span style={{ color: "#374151" }}>{r.toDoctorEmail}</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
