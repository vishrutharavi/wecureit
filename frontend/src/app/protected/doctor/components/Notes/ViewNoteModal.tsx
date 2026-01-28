"use client";

import React from "react";
import styles from "../../doctor.module.scss";
import { FiX } from "react-icons/fi";

type Note = {
  id: string;
  author: string;
  authorLicensed: boolean;
  date: string;
  text: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  patientName?: string;
  doctorName?: string;
  patientAge?: string;
};

export default function ViewNoteModal({ open, onClose, patientName = "Patient", doctorName = "Dr. You", patientAge }: Props) {
  // Only show notes authored by the current doctor and licensed
  const [selectedSpecialty, setSelectedSpecialty] = React.useState<string>("");

  const sampleNotes: Note[] = [
    { id: 'n1', author: 'Dr. Alice Park', authorLicensed: true, date: '2026-01-10', text: 'Follow-up after surgery: healing well.', },
    { id: 'n2', author: 'Dr. Bob Lee', authorLicensed: true, date: '2025-12-20', text: 'Initial consult: consider EKG.', },
    { id: 'n3', author: 'Dr. Alice Park', authorLicensed: true, date: '2025-11-05', text: 'Medication adjusted, monitor BP.', },
  ];

  if (!open) return null;

  // Filter: only notes by this doctor and licensed (authorLicensed assumed true for doctor notes)
  const doctorNotes = sampleNotes.filter(n => n.author === doctorName && n.authorLicensed === true);
  // Derive specialties from doctor's notes (if specialty metadata existed). For now, simulate specialties from note text tags.
  // Example mapping: if note text contains 'surgery' => 'Surgery', 'EKG' => 'Cardiology', 'Medication' => 'General'
  const specialties = Array.from(new Set(doctorNotes.map(n => {
    if (/surgery/i.test(n.text)) return 'Surgery';
    if (/ekg|cardio/i.test(n.text)) return 'Cardiology';
    if (/medica|medication|bp|blood pressure/i.test(n.text)) return 'General Medicine';
    return 'Other';
  })));

  const notes = doctorNotes.filter(n => {
    if (!selectedSpecialty) return true;
    // map note to same specialty classification used above
    let spec = 'Other';
    if (/surgery/i.test(n.text)) spec = 'Surgery';
    else if (/ekg|cardio/i.test(n.text)) spec = 'Cardiology';
    else if (/medica|medication|bp|blood pressure/i.test(n.text)) spec = 'General Medicine';
    return spec === selectedSpecialty;
  });

  return (
    <div className={styles['modal-overlay']} onClick={onClose}>
      <div className={styles['modal-container']} onClick={(e) => e.stopPropagation()}>
        <div className={styles['modal-header']}>
          <h2 className={styles['modal-title']}>View Notes</h2>
          <button onClick={onClose} aria-label="Close"><FiX size={18} /></button>
        </div>

        <div className={styles['modal-body']}>
          <div className={styles.viewHeaderRow}>
            <div>
              <div className={styles.cardTitle}>
                <div>{patientName}</div>
              </div>
              {patientAge ? <div className={`${styles.referralMeta} ${styles.mt6}`}>{patientAge}</div> : null}
              <div className={`${styles.compactRow} ${styles.mt6}`}>
                <div className={styles.referralMeta}>{doctorName ? `Dr. ${doctorName}` : 'Doctor'}</div>
                {selectedSpecialty ? (
                  <div className={styles.specialtyPill}>
                    {selectedSpecialty}
                  </div>
                ) : null}
              </div>
            </div>

            <div className={styles.compactRow}>
              <select value={selectedSpecialty} onChange={(e) => setSelectedSpecialty(e.target.value)} className={styles.filterSelect}>
                <option value="">All specialties</option>
                {specialties.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
          </div>

          <div className={styles.mt12}>
            {notes.length ? (
              notes.map(n => (
                <div key={n.id} className={`${styles.compactCard} ${styles.cardSpacing}`}>
                  <div className={`${styles.compactRow} ${styles.justifyBetween}`}>
                    <div>
                      <div className={styles.cardTitle}>{n.author} {n.authorLicensed ? <span className={styles.licenseBadgeGreen}>Licensed</span> : <span className={styles.licenseBadgeGray}>Unlicensed</span>}</div>
                      <div className={`${styles.referralMeta} ${styles.mt6}`}>{n.date}</div>
                    </div>
                  </div>
                  <div className={styles.noteText}>{n.text}</div>
                </div>
              ))
            ) : (
              <div className={styles.emptyCard}>No notes found</div>
            )}
          </div>
        </div>

        <div className={`${styles['modal-footer']} ${styles.modalFooterActions}`}>
          <button className={styles.secondaryBtn} onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
