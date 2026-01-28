"use client";

import React from "react";
import styles from "../../doctor.module.scss";

type Referral = {
  id: string;
  referringDoctor: string;
  referringDoctorEmail?: string;
  facility: string;
  patient: string;
  speciality: string;
  date: string;
  notes?: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  items: Referral[];
};

export default function ReferralModalNew({ open, onClose, items }: Props) {
  const [localItems, setLocalItems] = React.useState<Array<Referral & { isCancelled?: boolean; cancelReason?: string }>>(() =>
    items.map((i) => ({ ...i, isCancelled: false, cancelReason: "" }))
  );

  React.useEffect(() => {
    setLocalItems(items.map((i) => ({ ...i, isCancelled: false, cancelReason: "" })));
  }, [items]);

  const [doctorFilter, setDoctorFilter] = React.useState<string>("");
  const [facilityFilter, setFacilityFilter] = React.useState<string>("");
  const [specialtyFilter, setSpecialtyFilter] = React.useState<string>("");
  const [patientQuery, setPatientQuery] = React.useState<string>("");
  const [globalSearch, setGlobalSearch] = React.useState<string>("");
  // (create-referral UI removed) previously merged from Notes/ReferSpecialityModal

  const doctors = Array.from(new Set(localItems.map((i) => i.referringDoctor))).filter(Boolean);
  const facilities = Array.from(new Set(localItems.map((i) => i.facility))).filter(Boolean);
  const specialities = Array.from(new Set(localItems.map((i) => i.speciality))).filter(Boolean);

  const filtered = localItems.filter((r) => {
    if (doctorFilter && r.referringDoctor !== doctorFilter) return false;
    if (facilityFilter && r.facility !== facilityFilter) return false;
    if (specialtyFilter && r.speciality !== specialtyFilter) return false;
    if (patientQuery && !r.patient.toLowerCase().includes(patientQuery.toLowerCase())) return false;
    if (globalSearch) {
      const q = globalSearch.toLowerCase();
      const inPatient = r.patient.toLowerCase().includes(q);
      const inDoctor = r.referringDoctor.toLowerCase().includes(q);
      const inFacility = r.facility.toLowerCase().includes(q);
      const inSpec = r.speciality.toLowerCase().includes(q);
      if (!(inPatient || inDoctor || inFacility || inSpec)) return false;
    }
    return true;
  });

  if (!open) return null;

  return (
    <div className={styles["modal-overlay"]} onClick={onClose}>
      <div className={`${styles["modal-container"]} ${styles.modalWide}`} onClick={(e) => e.stopPropagation()}>
        <div className={styles["modal-header"]}>
          <h2 className={styles["modal-title"]}>Referrals</h2>
        </div>

        <div className={styles["modal-body"]}>
          {/* create-referral UI removed per request */}
          <div className={styles.referralControls}>
            <div>
              <input
                className={styles.referralSearchInput}
                placeholder="Search referrals (patient, doctor, facility, specialty)"
                value={globalSearch}
                onChange={(e) => setGlobalSearch(e.target.value)}
              />
            </div>

            <div className={styles.referralSortBlock}>
              <div className={styles.referralSortLabel}>Search by</div>
              <div className={styles.referralSelectRow}>
                <div className={styles.selectFlex}>
                  <select className={styles.compactSelect} value={doctorFilter} onChange={(e) => setDoctorFilter(e.target.value)}>
                    <option value="">All doctors</option>
                    {doctors.map((d) => (
                      <option key={d} value={d}>
                        {d}
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.selectWide260}>
                  <select className={styles.compactSelect} value={facilityFilter} onChange={(e) => setFacilityFilter(e.target.value)}>
                    <option value="">All facilities</option>
                    {facilities.map((f) => (
                      <option key={f} value={f}>
                        {f}
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.selectWide240}>
                  <select className={styles.compactSelect} value={specialtyFilter} onChange={(e) => setSpecialtyFilter(e.target.value)}>
                    <option value="">All specialties</option>
                    {specialities.map((s) => (
                      <option key={s} value={s}>
                        {s}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </div>
          </div>

          <div className={styles.referralCountRow}>
            <div className={styles.referralCountLabel}>{filtered.length} referral(s)</div>
            <div>
              <button
                className={styles.secondaryBtn}
                onClick={() => {
                  setDoctorFilter("");
                  setFacilityFilter("");
                  setSpecialtyFilter("");
                  setPatientQuery("");
                }}
              >
                Clear
              </button>
            </div>
          </div>

          {filtered.length === 0 ? (
            <div className={styles.emptyCard}>No referrals match your filters</div>
          ) : (
            <div className={styles.referralGrid}>
              {filtered.map((r) => (
                <div key={r.id} className={`${styles.compactCard} ${styles.referCard}`}>
                  <div className={styles.referralCardRow}>
                    <div className={styles.referralCardLeft}>
                      <div className={styles.referralCardHeader}>
                        <div className={styles.appointmentPatientName}>{r.patient}</div>
                        <div className={styles.referralDate}>{r.date}</div>
                      </div>
                      <div className={styles.referralMeta}>{r.speciality} • {r.facility}</div>
                      {!r.isCancelled ? (
                        <div className={styles.referralCancelledBlock}>
                          <input
                            className={styles.referralCancelInput}
                            placeholder="Cancellation reason (optional)"
                            value={r.cancelReason ?? ""}
                            onChange={(e) => setLocalItems((prev) => prev.map((x) => (x.id === r.id ? { ...x, cancelReason: e.target.value } : x)))}
                          />
                          <button
                            className={`${styles.appointmentActionBtn} ${styles["cancel"]} ${styles.referralCancelBtn}`}
                            onClick={() => setLocalItems((prev) => prev.map((x) => (x.id === r.id ? { ...x, isCancelled: true } : x)))}
                          >
                            Cancel Referral
                          </button>
                        </div>
                      ) : (
                        <div className={styles.referralCancelledBlock}>
                          <div className={styles.cardTitle}>Cancelled</div>
                          {r.cancelReason ? (
                            <div className={styles.mt6}>{r.cancelReason}</div>
                          ) : (
                            <div className={`${styles.mt6} ${styles.referralMeta}`}>No reason provided</div>
                          )}
                          <button
                            className={styles.secondaryBtn + " " + styles.mt8}
                            onClick={() => setLocalItems((prev) => prev.map((x) => (x.id === r.id ? { ...x, isCancelled: false, cancelReason: "" } : x)))}
                          >
                            Undo
                          </button>
                        </div>
                      )}
                    </div>
                    <div className={styles.referralCardRight}>
                      <div className={styles.cardTitle}>{r.referringDoctor}</div>
                      {r.referringDoctorEmail ? (
                        <div className={styles.mt6}>
                          <a className={styles.referralEmailLink} href={`mailto:${r.referringDoctorEmail}`}>
                            {r.referringDoctorEmail}
                          </a>
                        </div>
                      ) : null}
                      {r.notes ? <div className={styles.mt8}>{r.notes}</div> : null}
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
