"use client";

import React from "react";
import styles from "../../patient.module.scss";
// router not needed here (modal handles navigation)
import AppointmentConfirmationModal from "../AppointmentConfirmation/AppointmentConfirmationModal";

type Booking = {
  doctor?: { id?: string; name?: string } | null;
  facility?: { id?: string; name?: string; address?: string } | null;
  specialty?: { id?: string; name?: string } | null;
  date?: string | null;
  time?: string | null;
  duration?: number | null;
};

export default function CostSummary() {
  const [booking, setBooking] = React.useState<Booking | null>(null);
  const [hasCard, setHasCard] = React.useState(false);
  const [showModal, setShowModal] = React.useState(false);
  const [confirmation, setConfirmation] = React.useState("");

  React.useEffect(() => {
    try {
      const raw = sessionStorage.getItem("bookingSelection");
      if (raw) setBooking(JSON.parse(raw));
    } catch {}

    const updateHasCard = () => {
      try {
        const raw = localStorage.getItem("patientProfile");
        const p = raw ? JSON.parse(raw) : {};
        const cards = p?.payment?.cards || [];
        setHasCard(Array.isArray(cards) && cards.length > 0);
      } catch {
        setHasCard(false);
      }
    };

    // initial check
    updateHasCard();

    // listen for same-window updates dispatched by Payment component
    window.addEventListener('patient:cardsUpdated', updateHasCard);
    // also listen to storage for cross-window updates
    const storageHandler = (e: StorageEvent) => {
      if (e.key === 'patientProfile') updateHasCard();
    };
    window.addEventListener('storage', storageHandler);

    return () => {
      window.removeEventListener('patient:cardsUpdated', updateHasCard);
      window.removeEventListener('storage', storageHandler);
    };
  }, []);

  const duration = booking?.duration || 30;
  const ratePerMin = 4; // simple rate used for demo: $4 per minute
  const consultation = duration * ratePerMin;
  const facilityFee = 25;
  const subtotal = consultation + facilityFee;
  const tax = Math.round(subtotal * 0.08 * 100) / 100;
  const total = Math.round((subtotal + tax) * 100) / 100;

  return (
    <>
      <div className={styles.summaryWrapper}>
      <div className={styles.sectionTitle}>Cost Summary</div>
      {!hasCard ? (
        <div className={styles.noticeBox}>
          <div className={styles.noticeBoxTitle}>Please add a payment card to complete your booking.</div>
        </div>
      ) : null}

      <div className={`${styles.summaryCard} ${styles.mt12}`}>
        <div className={styles.fieldLabel}>Consultation Fee ({duration} min)</div>
        <div className={styles.amountRight}>${consultation.toFixed(2)}</div>
      </div>

      <div className={styles.summaryCard}>
        <div className={styles.fieldLabel}>Facility Fee</div>
        <div className={styles.amountRight}>${facilityFee.toFixed(2)}</div>
      </div>

      <div className={styles.totalsContainer}>
        <div className={styles.totalsRow}>
          <div>Subtotal</div>
          <div>${subtotal.toFixed(2)}</div>
        </div>
        <div className={styles.totalsTax}>
          <div>Tax (8%)</div>
          <div>${tax.toFixed(2)}</div>
        </div>
      </div>

      <div className={styles.totalAmountRow}>
        <div style={{ fontWeight: 800 }}>Total Amount</div>
        <div className={styles.totalValue}>${total.toFixed(2)}</div>
      </div>

      <div className={styles.mt12}>
        <button
          className={styles.continueBtn}
          disabled={!hasCard}
          onClick={() => {
                if (!hasCard) return;
                // TODO: replace with real booking API call. For now show confirmation modal.
                const id = 'WC' + String(Date.now()).slice(-9);
                setConfirmation(id);
                setShowModal(true);
          }}
        >
          Book Appointment
        </button>
      </div>
      </div>

      {showModal && (
        <AppointmentConfirmationModal
          open={showModal}
          onClose={() => setShowModal(false)}
          booking={booking}
          confirmation={confirmation}
        />
      )}
    </>
  );
}
