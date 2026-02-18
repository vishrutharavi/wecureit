"use client";

import React, { useState, useEffect, useCallback } from "react";
import styles from "../../doctor.module.scss";
import { Search } from "lucide-react";
import {
  getReferralSpecialities,
  getRecommendedDoctors,
  searchDoctorsForReferral,
  createReferral,
} from "@/lib/doctor/doctorApi";

type Speciality = { specialityCode: string; specialityName: string };

type RecommendedDoctor = {
  doctorId: string;
  doctorName: string;
  doctorEmail: string;
  stateName: string;
  stateCode: string;
  nextAvailableSlot: string;
  appointmentLoad: number;
  reason: string;
  score: number;
};

function ScoreBadge({ score }: { score: number }) {
  const color = score >= 70 ? "#16a34a" : score >= 40 ? "#d97706" : "#dc2626";
  const bg    = score >= 70 ? "#dcfce7"  : score >= 40 ? "#fef3c7"  : "#fee2e2";
  const label = score >= 70 ? "Great"    : score >= 40 ? "Good"     : "Fair";
  return (
    <span style={{
      display: "inline-flex", alignItems: "center", gap: 4,
      padding: "2px 8px", borderRadius: 999,
      background: bg, color, fontSize: "0.75rem", fontWeight: 700,
    }}>
      {score}% &bull; {label}
    </span>
  );
}

type Props = {
  open: boolean;
  onClose: () => void;
  patientName: string;
  patientId?: string;
  appointmentDbId?: string;
};

export default function CreateReferralModal({
  open,
  onClose,
  patientName,
  patientId,
  appointmentDbId,
}: Props) {
  const [specialities, setSpecialities] = useState<Speciality[]>([]);
  const [selectedSpeciality, setSelectedSpeciality] = useState("");
  const [recommendations, setRecommendations] = useState<RecommendedDoctor[]>([]);
  const [searchResults, setSearchResults] = useState<RecommendedDoctor[]>([]);
  const [selectedDoctorId, setSelectedDoctorId] = useState("");
  const [reason, setReason] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [loading, setLoading] = useState(false);
  const [loadingRecs, setLoadingRecs] = useState(false);
  const [searching, setSearching] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  const getDoctorId = useCallback(() => {
    try {
      const raw = localStorage.getItem("doctorProfile");
      if (raw) return JSON.parse(raw).id;
    } catch {}
    return null;
  }, []);

  // Load specialities when modal opens
  useEffect(() => {
    if (!open) return;
    let mounted = true;
    async function load() {
      const doctorId = getDoctorId();
      if (!doctorId) return;
      try {
        setLoading(true);
        const data = await getReferralSpecialities(doctorId);
        if (mounted && Array.isArray(data)) setSpecialities(data);
      } catch {
        // ignore
      } finally {
        if (mounted) setLoading(false);
      }
    }
    load();
    return () => { mounted = false; };
  }, [open, getDoctorId]);

  // Load recommendations when speciality changes
  useEffect(() => {
    if (!selectedSpeciality || !patientId) {
      setRecommendations([]);
      return;
    }
    let mounted = true;
    async function load() {
      const doctorId = getDoctorId();
      if (!doctorId) return;
      try {
        setLoadingRecs(true);
        setRecommendations([]);
        setSelectedDoctorId("");
        const data = await getRecommendedDoctors(doctorId, patientId!, selectedSpeciality);
        if (mounted && Array.isArray(data)) setRecommendations(data);
      } catch {
        // ignore
      } finally {
        if (mounted) setLoadingRecs(false);
      }
    }
    load();
    return () => { mounted = false; };
  }, [selectedSpeciality, patientId, getDoctorId]);

  // Manual doctor search
  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    const doctorId = getDoctorId();
    if (!doctorId) return;
    try {
      setSearching(true);
      const data = await searchDoctorsForReferral(doctorId, searchQuery, selectedSpeciality || undefined);
      if (Array.isArray(data)) setSearchResults(data);
    } catch {
      // ignore
    } finally {
      setSearching(false);
    }
  };

  const handleSubmit = async () => {
    if (!selectedDoctorId) { setError("Please select a doctor"); return; }
    if (!reason.trim()) { setError("Please enter a reason for referral"); return; }
    if (!patientId) { setError("Patient ID is missing"); return; }

    const doctorId = getDoctorId();
    if (!doctorId) { setError("Doctor not authenticated"); return; }

    try {
      setSubmitting(true);
      setError(null);
      await createReferral(doctorId, {
        patientId,
        toDoctorId: selectedDoctorId,
        appointmentId: appointmentDbId ? Number(appointmentDbId) : undefined,
        specialityCode: selectedSpeciality,
        reason: reason.trim(),
      });
      setSuccess(true);
      setTimeout(() => {
        resetForm();
        onClose();
      }, 1500);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create referral");
    } finally {
      setSubmitting(false);
    }
  };

  const resetForm = () => {
    setSelectedSpeciality("");
    setRecommendations([]);
    setSearchResults([]);
    setSelectedDoctorId("");
    setReason("");
    setSearchQuery("");
    setError(null);
    setSuccess(false);
  };

  const handleClose = () => {
    resetForm();
    onClose();
  };

  if (!open) return null;

  const allDoctors = [
    ...recommendations,
    ...searchResults.filter((s) => !recommendations.some((r) => r.doctorId === s.doctorId)),
  ];

  return (
    <div className={styles["modal-overlay"]} onClick={handleClose}>
      <div className={`${styles["modal-container"]} ${styles.modalWide}`} onClick={(e) => e.stopPropagation()}>
        <div className={styles["modal-header"]}>
          <h2 className={styles["modal-title"]}>Refer Patient</h2>
          <button onClick={handleClose} aria-label="Close" className={styles.modalCloseBtn}>&times;</button>
        </div>

        <div className={styles["modal-body"]}>
          {success ? (
            <div className={styles.referralSuccessMsg}>
              Referral created successfully for {patientName}!
            </div>
          ) : (
            <>
              {/* Patient Info */}
              <div className={styles.referralSection}>
                <div className={styles.referralSectionLabel}>Patient</div>
                <div className={styles.referralPatientName}>{patientName}</div>
              </div>

              {/* Section A — Specialty */}
              <div className={styles.referralSection}>
                <div className={styles.referralSectionLabel}>Referral Specialty <span className={styles.required}>*</span></div>
                {loading ? (
                  <div className={styles.referralMeta}>Loading specialities...</div>
                ) : (
                  <select
                    className={styles.compactSelect}
                    value={selectedSpeciality}
                    onChange={(e) => {
                      setSelectedSpeciality(e.target.value);
                      setSearchResults([]);
                      setSelectedDoctorId("");
                    }}
                  >
                    <option value="">Select a specialty</option>
                    {specialities.map((s) => (
                      <option key={s.specialityCode} value={s.specialityCode}>
                        {s.specialityName}
                      </option>
                    ))}
                  </select>
                )}
              </div>

              {/* Section B — Recommended Doctors */}
              {selectedSpeciality && (
                <div className={styles.referralSection}>
                  <div className={styles.referralSectionLabel}>Recommended Specialists</div>
                  {loadingRecs ? (
                    <div className={styles.referralMeta}>Finding recommended doctors...</div>
                  ) : recommendations.length === 0 ? (
                    <div className={styles.referralMeta}>No recommendations found for this specialty in the patient&apos;s state. Try manual search below.</div>
                  ) : (
                    <div className={styles.recommendedDoctorList}>
                      {recommendations.map((doc) => (
                        <label key={doc.doctorId} className={`${styles.recommendedDoctorItem} ${selectedDoctorId === doc.doctorId ? styles.selected : ""}`}>
                          <input
                            type="radio"
                            name="selectedDoctor"
                            value={doc.doctorId}
                            checked={selectedDoctorId === doc.doctorId}
                            onChange={() => setSelectedDoctorId(doc.doctorId)}
                          />
                          <div className={styles.recommendedDoctorInfo}>
                            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                              <div className={styles.recommendedDoctorName}>{doc.doctorName}</div>
                              <ScoreBadge score={doc.score ?? 0} />
                            </div>
                            <div className={styles.recommendedDoctorMeta}>
                              {doc.stateName || doc.stateCode} &bull; Next slot: {doc.nextAvailableSlot}
                            </div>
                            <div className={styles.recommendedDoctorReason}>{doc.reason}</div>
                          </div>
                        </label>
                      ))}
                    </div>
                  )}
                </div>
              )}

              {/* Section C — Manual Search */}
              <div className={styles.referralSection}>
                <div className={styles.referralSectionLabel}>Search Doctor Manually</div>
                <div className={styles.referralSearchRow}>
                  <input
                    className={styles.compactSelect}
                    placeholder="Search by doctor name..."
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    onKeyDown={(e) => { if (e.key === "Enter") handleSearch(); }}
                    style={{ flex: 1 }}
                  />
                  <button className={styles.secondaryBtn} onClick={handleSearch} disabled={searching}>
                    <Search size={14} /> {searching ? "Searching..." : "Search"}
                  </button>
                </div>
                {searchResults.length > 0 && (
                  <div className={styles.recommendedDoctorList} style={{ marginTop: 8 }}>
                    {searchResults.map((doc) => (
                        <label key={doc.doctorId} className={`${styles.recommendedDoctorItem} ${selectedDoctorId === doc.doctorId ? styles.selected : ""}`}>
                          <input
                            type="radio"
                            name="selectedDoctor"
                            value={doc.doctorId}
                            checked={selectedDoctorId === doc.doctorId}
                            onChange={() => setSelectedDoctorId(doc.doctorId)}
                          />
                          <div className={styles.recommendedDoctorInfo}>
                            <div className={styles.recommendedDoctorName}>{doc.doctorName}</div>
                            <div className={styles.recommendedDoctorMeta}>
                              {doc.stateName || doc.stateCode || ""}
                            </div>
                          </div>
                        </label>
                      ))}
                  </div>
                )}
              </div>

              {/* Selected doctor display */}
              {selectedDoctorId && (
                <div className={styles.referralSection}>
                  <div className={styles.referralSectionLabel}>Selected Doctor</div>
                  <div className={styles.referralSelectedDoctor}>
                    {allDoctors.find((d) => d.doctorId === selectedDoctorId)?.doctorName || "Unknown"}
                  </div>
                </div>
              )}

              {/* Section D — Reason */}
              <div className={styles.referralSection}>
                <div className={styles.referralSectionLabel}>Reason for Referral <span className={styles.required}>*</span></div>
                <textarea
                  className={styles.referralReasonTextarea}
                  placeholder="Enter the reason for this referral..."
                  value={reason}
                  onChange={(e) => setReason(e.target.value)}
                  rows={3}
                />
              </div>

              {error && <div className={styles.referralError}>{error}</div>}
            </>
          )}
        </div>

        {!success && (
          <div className={styles["modal-footer"]}>
            <button className={styles.secondaryBtn} onClick={handleClose}>
              Cancel
            </button>
            <button
              className={styles.viewAppointmentsBtn}
              onClick={handleSubmit}
              disabled={submitting || !selectedDoctorId || !reason.trim()}
            >
              {submitting ? "Creating..." : "Create Referral"}
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
