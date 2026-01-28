"use client";

import React from "react";
import styles from "../../doctor.module.scss";
import { FiX } from "react-icons/fi";

type Props = {
  open: boolean;
  onClose: () => void;
  patientName?: string;
  dateLabel?: string;
  timeLabel?: string;
};

export default function AddNoteModal({ open, onClose, patientName = "Patient", dateLabel = "", timeLabel = "" }: Props) {
  const [note, setNote] = React.useState("");
  if (!open) return null;

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
          <button className={styles.secondaryBtn} onClick={onClose}>Cancel</button>
          <button className={styles.viewAppointmentsBtn} onClick={() => { /* TODO: persist note */ onClose(); }}>Save Note</button>
        </div>
      </div>
    </div>
  );
}
