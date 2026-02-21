"use client";

import React from "react";
import styles from "../../doctor.module.scss";
import { getClinicalNotes } from "@/lib/doctor/doctorApi";
import { FiX } from "react-icons/fi";

type Note = {
  id: string;
  author: string;
  authorLicensed: boolean;
  date: string;
  text: string;
  patientAge?: string;
  patientSex?: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  patientName?: string;
  doctorName?: string;
  patientAge?: string;
  patientSex?: string;
  appointmentDbId?: string;
  patientId?: string;
};
export default function ViewNoteModal({ open, onClose, patientName = "Patient", doctorName, patientAge, patientSex, appointmentDbId, patientId }: Props) {
  // live notes fetched from backend for the appointment; falling back to patient-wide if needed
  const [selectedSpecialty, setSelectedSpecialty] = React.useState<string>("");
  const [notes, setNotes] = React.useState<Note[]>([]);
  const [displayAge, setDisplayAge] = React.useState<string | undefined>(patientAge);
  const [displaySex, setDisplaySex] = React.useState<string | undefined>(patientSex);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const doctorNameDep = doctorName ?? '';

  React.useEffect(() => {
    if (!open) return;
    async function load() {
      setLoading(true);
      setError(null);
      try {
        const token = typeof window !== 'undefined' ? (localStorage.getItem('doctorToken') ?? undefined) : undefined;
        const data = await getClinicalNotes({ appointmentDbId, patientId }, token);
        // Map backend ClinicalNote to Note for UI
        const mapped: Note[] = (data || []).map((n: Record<string, unknown>) => {
          const rawAuthor = (n['createdBy'] as string) ?? (n['doctorId'] ? `Doctor ${String(n['doctorId'])}` : 'Unknown');
          // Consider this note authored by a doctor if doctorId is present, or if it matches the modal doctorName
          const isDoctorAuthor = !!n['doctorId'] || (doctorNameDep && String(rawAuthor) === doctorNameDep);
          const author = isDoctorAuthor && !/^Dr\b|^Doctor\b/i.test(String(rawAuthor)) ? `Dr. ${rawAuthor}` : rawAuthor;
          const out: Note = {
            id: String(n['id']),
            author,
            authorLicensed: true,
            date: n['createdAt'] ? new Date(String(n['createdAt'])).toLocaleDateString() : '',
            text: (n['noteText'] as string) ?? (n['text'] as string) ?? '',
            patientAge: n['patientAge'] as string | undefined,
            patientSex: n['patientSex'] as string | undefined,
          };
          // if the modal wasn't passed patientAge/sex props, take from first note
          return out;
        });
        setNotes(mapped);
        // populate header age/sex from first note if not already provided
        try {
          if ((!patientAge || !patientSex) && mapped.length > 0) {
            const first = mapped[0];
            if (!patientAge && first.patientAge) setDisplayAge(first.patientAge);
            if (!patientSex && first.patientSex) setDisplaySex(first.patientSex);
          }
        } catch {}
      } catch (err: unknown) {
  const msg = (err && typeof err === 'object' && 'message' in (err as Record<string, unknown>)) ? String((err as Record<string, unknown>)['message']) : String(err);
        setError(msg);
        setNotes([]);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [open, appointmentDbId, patientId, doctorNameDep, patientAge, patientSex]);

  if (!open) return null;

  // derive specialties from notes text (best-effort)
  const specialties = Array.from(new Set(notes.map(n => {
    if (/surgery/i.test(n.text)) return 'Surgery';
    if (/ekg|cardio/i.test(n.text)) return 'Cardiology';
    if (/medica|medication|bp|blood pressure/i.test(n.text)) return 'General Medicine';
    return 'Other';
  })));

  const visibleNotes = notes.filter(n => {
    if (!selectedSpecialty) return true;
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
              {(displayAge || displaySex) ? (
                <div className={`${styles.referralMeta} ${styles.mt6}`}>
                  {displayAge ? <span>{displayAge}</span> : null}
                  {displayAge && displaySex ? <span>{' • '}</span> : null}
                  {displaySex ? <span>{displaySex}</span> : null}
                </div>
              ) : null}
            </div>

            <div className={styles.compactRow}>
              <select value={selectedSpecialty} onChange={(e) => setSelectedSpecialty(e.target.value)} className={styles.filterSelect}>
                <option value="">All specialties</option>
                {specialties.map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
          </div>

          <div className={styles.mt12}>
            {loading ? (
              <div className={styles.emptyCard}>Loading notes…</div>
            ) : error ? (
              <div className={styles.emptyCard}>Error loading notes: {error}</div>
            ) : visibleNotes.length ? (
              visibleNotes.map(n => (
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
