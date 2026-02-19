"use client";

import React, { useState, useEffect, useCallback } from "react";
import styles from "../../doctor.module.scss";
import {
  getOutgoingReferrals,
  getIncomingReferrals,
  cancelReferral,
  acceptReferral,
  completeReferral,
} from "@/lib/doctor/doctorApi";

type ReferralItem = {
  id: string;
  patientName: string;
  patientId: string;
  fromDoctorId: string;
  fromDoctorName: string;
  fromDoctorEmail: string;
  toDoctorId: string;
  toDoctorName: string;
  toDoctorEmail: string;
  specialityCode: string;
  specialityName: string;
  reason: string;
  status: string;
  cancelReason?: string;
  createdAt: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
};

export default function ReferralModal({ open, onClose }: Props) {
  const [tab, setTab] = useState<"outgoing" | "incoming">("outgoing");
  const [outgoing, setOutgoing] = useState<ReferralItem[]>([]);
  const [incoming, setIncoming] = useState<ReferralItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [globalSearch, setGlobalSearch] = useState("");
  const [specialtyFilter, setSpecialtyFilter] = useState("");
  const [cancelReasons, setCancelReasons] = useState<Record<string, string>>({});

  const getDoctorId = useCallback(() => {
    try {
      const raw = localStorage.getItem("doctorProfile");
      if (raw) return JSON.parse(raw).id;
    } catch {}
    return null;
  }, []);

  const fetchReferrals = useCallback(async () => {
    const doctorId = getDoctorId();
    if (!doctorId) return;
    try {
      setLoading(true);
      const [out, inc] = await Promise.all([
        getOutgoingReferrals(doctorId),
        getIncomingReferrals(doctorId),
      ]);
      if (Array.isArray(out)) setOutgoing(out);
      if (Array.isArray(inc)) setIncoming(inc);
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }, [getDoctorId]);

  useEffect(() => {
    if (open) fetchReferrals();
  }, [open, fetchReferrals]);

  const handleCancel = async (referralId: string) => {
    const doctorId = getDoctorId();
    if (!doctorId) return;
    try {
      await cancelReferral(doctorId, referralId, cancelReasons[referralId] || undefined);
      fetchReferrals();
    } catch {
      // ignore
    }
  };

  const handleAccept = async (referralId: string) => {
    const doctorId = getDoctorId();
    if (!doctorId) return;
    try {
      await acceptReferral(doctorId, referralId);
      fetchReferrals();
    } catch {
      // ignore
    }
  };

  const handleComplete = async (referralId: string) => {
    const doctorId = getDoctorId();
    if (!doctorId) return;
    try {
      await completeReferral(doctorId, referralId);
      fetchReferrals();
    } catch {
      // ignore
    }
  };

  const items = tab === "outgoing" ? outgoing : incoming;

  const specialities = Array.from(new Set(items.map((i) => i.specialityName).filter(Boolean)));

  const filtered = items.filter((r) => {
    if (specialtyFilter && r.specialityName !== specialtyFilter) return false;
    if (globalSearch) {
      const q = globalSearch.toLowerCase();
      const inPatient = (r.patientName || "").toLowerCase().includes(q);
      const inFrom = (r.fromDoctorName || "").toLowerCase().includes(q);
      const inTo = (r.toDoctorName || "").toLowerCase().includes(q);
      const inSpec = (r.specialityName || "").toLowerCase().includes(q);
      if (!(inPatient || inFrom || inTo || inSpec)) return false;
    }
    return true;
  });

  if (!open) return null;

  const totalCount = outgoing.length + incoming.length;

  return (
    <div className={styles["modal-overlay"]} onClick={onClose}>
      <div className={`${styles["modal-container"]} ${styles.modalWide}`} onClick={(e) => e.stopPropagation()}>
        <div className={styles["modal-header"]}>
          <h2 className={styles["modal-title"]}>My Referrals ({totalCount})</h2>
        </div>

        <div className={styles["modal-body"]}>
          {/* Tabs */}
          <div className={styles.referralTabRow}>
            <button
              className={`${styles.tab} ${tab === "outgoing" ? styles.active : ""}`}
              onClick={() => { setTab("outgoing"); setSpecialtyFilter(""); setGlobalSearch(""); }}
            >
              Outgoing ({outgoing.length})
            </button>
            <button
              className={`${styles.tab} ${tab === "incoming" ? styles.active : ""}`}
              onClick={() => { setTab("incoming"); setSpecialtyFilter(""); setGlobalSearch(""); }}
            >
              Incoming ({incoming.length})
            </button>
          </div>

          {/* Filters */}
          <div className={styles.referralControls}>
            <div>
              <input
                className={styles.referralSearchInput}
                placeholder="Search referrals (patient, doctor, specialty)"
                value={globalSearch}
                onChange={(e) => setGlobalSearch(e.target.value)}
              />
            </div>
            <div className={styles.referralSortBlock}>
              <div className={styles.referralSelectRow}>
                <div className={styles.selectWide240}>
                  <select className={styles.compactSelect} value={specialtyFilter} onChange={(e) => setSpecialtyFilter(e.target.value)}>
                    <option value="">All specialties</option>
                    {specialities.map((s) => (
                      <option key={s} value={s}>{s}</option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
          </div>

          <div className={styles.referralCountRow}>
            <div className={styles.referralCountLabel}>{filtered.length} referral(s)</div>
            <button className={styles.secondaryBtn} onClick={() => { setSpecialtyFilter(""); setGlobalSearch(""); }}>
              Clear
            </button>
          </div>

          {loading ? (
            <div className={styles.emptyCard}>Loading referrals...</div>
          ) : filtered.length === 0 ? (
            <div className={styles.emptyCard}>
              {items.length === 0
                ? `No ${tab} referrals yet`
                : "No referrals match your filters"}
            </div>
          ) : (
            <div className={styles.referralGrid}>
              {filtered.map((r) => (
                <div key={r.id} className={`${styles.compactCard} ${styles.referCard}`}>
                  <div className={styles.referralCardRow}>
                    <div className={styles.referralCardLeft}>
                      <div className={styles.referralCardHeader}>
                        <div className={styles.appointmentPatientName}>{r.patientName}</div>
                        <div className={styles.referralDate}>
                          {r.createdAt ? new Date(r.createdAt).toLocaleDateString() : ""}
                        </div>
                      </div>
                      <div className={styles.referralMeta}>
                        {r.specialityName} &bull;{" "}
                        <span className={`${styles.smallBadge} ${r.status === "CANCELLED" ? styles.cancelledBadge : ""}`}>
                          {r.status}
                        </span>
                      </div>
                      {r.reason && (
                        <div className={styles.referralReasonDisplay}>{r.reason}</div>
                      )}
                      {/* Cancel controls — only for outgoing & non-cancelled */}
                      {tab === "outgoing" && r.status !== "CANCELLED" && (
                        <div className={styles.referralCancelledBlock}>
                          <input
                            className={styles.referralCancelInput}
                            placeholder="Cancellation reason (optional)"
                            value={cancelReasons[r.id] ?? ""}
                            onChange={(e) => setCancelReasons((prev) => ({ ...prev, [r.id]: e.target.value }))}
                          />
                          <button
                            className={`${styles.appointmentActionBtn} ${styles["cancel"]} ${styles.referralCancelBtn}`}
                            onClick={() => handleCancel(r.id)}
                          >
                            Cancel Referral
                          </button>
                        </div>
                      )}
                      {/* Accept/Complete controls — only for incoming referrals */}
                      {tab === "incoming" && r.status === "PENDING" && (
                        <div className={styles.referralCancelledBlock}>
                          <button
                            className={styles.viewAppointmentsBtn}
                            onClick={() => handleAccept(r.id)}
                          >
                            Accept Referral
                          </button>
                        </div>
                      )}
                      {tab === "incoming" && r.status === "ACCEPTED" && (
                        <div className={styles.referralCancelledBlock}>
                          <button
                            className={styles.viewAppointmentsBtn}
                            onClick={() => handleComplete(r.id)}
                          >
                            Mark Completed
                          </button>
                        </div>
                      )}
                      {r.status === "CANCELLED" && r.cancelReason && (
                        <div className={styles.referralCancelledBlock}>
                          <div className={styles.referralMeta}>Cancelled: {r.cancelReason}</div>
                        </div>
                      )}
                    </div>
                    <div className={styles.referralCardRight}>
                      <div className={styles.cardTitle}>
                        {tab === "outgoing" ? `To: ${r.toDoctorName}` : `From: ${r.fromDoctorName}`}
                      </div>
                      <div className={styles.mt6}>
                        <a
                          className={styles.referralEmailLink}
                          href={`mailto:${tab === "outgoing" ? r.toDoctorEmail : r.fromDoctorEmail}`}
                        >
                          {tab === "outgoing" ? r.toDoctorEmail : r.fromDoctorEmail}
                        </a>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        <div className={styles["modal-footer"]}>
          <button className={styles.secondaryBtn} onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );
}
