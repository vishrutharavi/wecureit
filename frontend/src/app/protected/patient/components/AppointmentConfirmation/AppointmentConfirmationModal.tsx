"use client";

import React from "react";
import styles from "../../patient.module.scss";
import { useRouter } from "next/navigation";

type Booking = {
  doctor?: { id?: string; name?: string } | null;
  facility?: { id?: string; name?: string; address?: string } | null;
  specialty?: { id?: string; name?: string } | null;
  date?: string | null;
  time?: string | null;
  duration?: number | null;
  roomNumber?: string | null;
};

export default function AppointmentConfirmationModal({
  open,
  onClose,
  booking,
  confirmation,
}: {
  open: boolean;
  onClose: () => void;
  booking: Booking | null;
  confirmation: string;
}) {
  const router = useRouter();
  if (!open) return null;

  return (
    <div className={styles['modal-overlay']}>
      <div className={styles['modal-container']}>
        <div className={styles['modal-header']}>
          <h2 className={styles['modal-title']}>Appointment Confirmed!</h2>
          <button onClick={onClose} className={styles['modal-close'] ?? ''}>✕</button>
        </div>

        <div className={styles['modal-body']}>
          <div className={styles['confirmation-badge-wrap']}>
            <div className={styles['confirmation-badge']}>
              <div className={styles['confirmation-check']}>✓</div>
            </div>
          </div>

          <div className={styles['confirmation-heading']}>
            <div className={styles['confirmation-heading-title']}>Appointment Confirmed!</div>
            <div className={styles['confirmation-heading-sub']}>Your appointment has been successfully booked.</div>
          </div>

          <div className={styles['confirmation-number-box']}>
            <div className={styles['confirmation-number-label']}>Confirmation Number</div>
            <div className={styles['confirmation-number-value']}>{confirmation}</div>
          </div>

          <div className={styles['confirmation-grid']}>
            <div className={styles['confirmation-row-label']}>Doctor:</div>
            <div className={styles['confirmation-row-value']}>{booking?.doctor?.name || '—'}</div>

            <div className={styles['confirmation-row-label']}>Date:</div>
            <div className={styles['confirmation-row-value']}>{booking?.date || '—'}</div>

            <div className={styles['confirmation-row-label']}>Time:</div>
            <div className={styles['confirmation-row-value']}>{booking?.time ? `${booking.time} (${booking.duration || 30} min)` : '—'}</div>

            <div className={styles['confirmation-row-label']}>Location:</div>
            <div className={styles['confirmation-row-value']}>{booking?.facility?.name || '—'}</div>

            <div className={styles['confirmation-row-label']}>Room:</div>
            <div className={styles['confirmation-row-value']}>{booking?.roomNumber || '—'}</div>
          </div>

          <div className={styles['modal-footer']}>
            <button
              className={styles.continueBtn}
              onClick={() => {
                onClose();
                // Navigate to patient home page
                router.push('/protected/patient');
              }}
            >
              Return to Home
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
