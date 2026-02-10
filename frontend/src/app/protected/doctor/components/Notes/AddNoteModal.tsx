"use client";

import React from "react";
import styles from "../../doctor.module.scss";
import { apiFetch } from "../../../../../lib/api";
import { FiX } from "react-icons/fi";

type Props = {
  open: boolean;
  onClose: () => void;
  patientName?: string;
  dateLabel?: string;
  timeLabel?: string;
  appointmentDbId?: string; // numeric DB id as string
  patientId?: string; // patient UUID
};
export default function AddNoteModal({ open, onClose, patientName = "Patient", dateLabel = "", timeLabel = "", appointmentDbId, patientId }: Props) {
  const [note, setNote] = React.useState("");
  const [saving, setSaving] = React.useState(false);
  if (!open) return null;

  async function saveNote() {
    try {
      setSaving(true);
      // doctor info from localStorage
      const rawDoc = typeof window !== 'undefined' ? localStorage.getItem('doctorProfile') : null;
      const doc = rawDoc ? JSON.parse(rawDoc) : null;
      const doctorId = doc?.id;
      const createdBy = doc?.name ?? undefined;

  const payload: Record<string, unknown> = { noteText: note };
      if (appointmentDbId) payload.appointmentDbId = appointmentDbId;
      if (patientId) payload.patientId = patientId;
      if (doctorId) payload.doctorId = doctorId;
      if (createdBy) payload.createdBy = createdBy;

      try {
        const saved = await apiFetch('/api/clinical-notes', localStorage.getItem('doctorToken') ?? undefined, {
          method: 'POST',
          body: JSON.stringify(payload),
        });
        console.log('Clinical note saved', saved);
      } catch (e) {
        console.error('Save note failed', e);
        // optionally show error UI here
      }
      setSaving(false);
      onClose();
    } catch (err) {
      console.error('Save note error', err);
      setSaving(false);
      onClose();
    }
  }

  return (
    <div className={styles["modal-overlay"]} onClick={onClose}>
      <div className={`${styles["modal-container"]} ${styles.modalMid}`} onClick={(e) => e.stopPropagation()}>
        <div className={styles["modal-header"]}>
          <h2 className={styles["modal-title"]}>Clinical Note - {patientName}</h2>
          <button onClick={onClose} aria-label="Close"><FiX size={18} /></button>
        </div>

        <div className={styles["modal-body"]}>
          <div className={`${styles.compactCard} ${styles.referCard}`}>
            <div className={styles.compactRow}>
              <div>
                <div className={styles.cardTitle}>Patient: {patientName}</div>
                <div className={`${styles.referralMeta} ${styles.mt6}`}>{dateLabel} {timeLabel ? ' • ' + timeLabel : ''}</div>
              </div>
            </div>
          </div>

          <div className={styles.mt12} />

          <textarea
            className={`${styles.noteTextarea} ${styles.noteTextareaLarge}`}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            placeholder={`Enter your clinical notes here...\n\nExample format:\n- Chief Complaint\n- History of Present Illness\n- Physical Examination\n- Assessment\n- Plan`}
          />
        </div>

        <div className={`${styles["modal-footer"]} ${styles.modalFooterActions}`}>
          <button className={styles.secondaryBtn} onClick={onClose} disabled={saving}>Cancel</button>
          <button className={styles.viewAppointmentsBtn} onClick={saveNote} disabled={saving}>{saving ? 'Saving…' : 'Save Note'}</button>
        </div>
      </div>
    </div>
  );
}
