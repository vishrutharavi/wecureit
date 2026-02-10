"use client";

import React from "react";
import styles from "../../patient.module.scss";
import { apiFetch } from '@/lib/api';
import { useRouter } from 'next/navigation';
// router not needed here (modal handles navigation)
import AppointmentConfirmationModal from "../AppointmentConfirmation/AppointmentConfirmationModal";

type Booking = {
  doctor?: { id?: string; name?: string } | null;
  facility?: { id?: string; name?: string; address?: string } | null;
  specialty?: { id?: string; name?: string } | null;
  date?: string | null;
  time?: string | null;
  duration?: number | null;
  doctorAvailabilityId?: string | null;
  roomScheduleId?: string | null;
  chiefComplaints?: string | null;
  roomNumber?: string | null;
};

export default function CostSummary() {
  const router = useRouter();
  const [booking, setBooking] = React.useState<Booking | null>(null);
  const [hasCard, setHasCard] = React.useState(false);
  const [showModal, setShowModal] = React.useState(false);
  const [confirmation, setConfirmation] = React.useState("");
  const [bookingInProgress, setBookingInProgress] = React.useState(false);

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
          disabled={!hasCard || bookingInProgress}
          onClick={async () => {
            if (!hasCard || bookingInProgress) return;
            setBookingInProgress(true);
            try {
              // ensure we have the latest booking selection
              let payload: Booking | null = booking;
              try {
                const raw = sessionStorage.getItem('bookingSelection');
                if (raw) payload = JSON.parse(raw) as Booking;
              } catch {}

              if (!payload) throw new Error('No booking selection found');

              // resolve patient id from localStorage if available, otherwise call /api/patient/me
              let patientId: string | undefined;
              try {
                const raw = localStorage.getItem('patientProfile');
                if (raw) {
                  const p = JSON.parse(raw);
                  if (p && p.id) patientId = String(p.id);
                }
              } catch {}

              if (!patientId) {
                const me = await apiFetch('/api/patient/me');
                if (me && me.id) patientId = String(me.id);
              }
              if (!patientId) throw new Error('Unable to determine patient id');

              // build appointment request body
              const dateStr = payload.date; // expected YYYY-MM-DD
              const timeLabel = payload.time; // expected format like '2:30 PM'
              // helper: parse time label into hours/minutes
              const parseTimeLabel = (lbl?: string) => {
                if (!lbl) return null;
                const m = lbl.match(/(\d{1,2}):(\d{2})\s*(AM|PM)/i);
                if (!m) return null;
                let hh = parseInt(m[1], 10);
                const mm = parseInt(m[2], 10);
                const ampm = (m[3] || '').toUpperCase();
                const isPM = ampm === 'PM';
                if (hh === 12) hh = isPM ? 12 : 0;
                if (isPM && hh !== 12) hh += 12;
                return { hh, mm };
              };

              const t = parseTimeLabel(timeLabel ?? undefined);
              if (!t) throw new Error('Invalid time selected');

              // construct local datetime strings without timezone (backend expects local datetime)
              const pad = (n: number) => String(n).padStart(2, '0');
              const startLocal = `${dateStr}T${pad(t.hh)}:${pad(t.mm)}:00`;
              // compute end time by adding duration minutes
              const startDateObj = new Date(`${dateStr}T${pad(t.hh)}:${pad(t.mm)}:00`);
              const endDateObj = new Date(startDateObj.getTime() + (duration || 30) * 60000);
              const endLocal = `${endDateObj.getFullYear()}-${pad(endDateObj.getMonth() + 1)}-${pad(endDateObj.getDate())}T${pad(endDateObj.getHours())}:${pad(endDateObj.getMinutes())}:00`;

                const body = {
                date: dateStr,
                duration: duration,
                patientId: patientId,
                doctorAvailabilityId: payload?.doctorAvailabilityId ?? null,
                startTime: startLocal,
                endTime: endLocal,
                facilityId: payload?.facility?.id ?? null,
                specialityId: payload?.specialty?.id ?? null,
                roomScheduleId: payload?.roomScheduleId ?? null,
                isActive: true,
                chiefComplaints: payload?.chiefComplaints ?? null,
              };

              const res = await apiFetch('/appointments/create', undefined, {
                method: 'POST',
                body: JSON.stringify(body),
              });

              // res expected to be AppointmentResponse with id
              const apptId = res && res.id ? String(res.id) : null;
              const conf = apptId ? `WC${apptId}` : `WC${String(Date.now()).slice(-9)}`;
              setConfirmation(conf);
              // show modal with booking details returned from storage (no room details)
              const modalBooking: Booking = { ...(booking ?? {}) };
              // update booking shown in modal (do not overwrite session storage)
              setBooking(modalBooking);
              setShowModal(true);
            } catch (err: unknown) {
              let msg = '';
              if (typeof err === 'string') msg = err;
              else if (err && typeof err === 'object' && 'message' in err) {
                const m = (err as { message?: unknown }).message;
                msg = typeof m === 'string' ? m : String(m);
              } else {
                msg = String(err ?? '');
              }
              // If duplicate (server returned 409), guide user to upcoming appointments
              if (/409|already exists|already booked|Appointment already exists/i.test(msg)) {
                try { window.alert('This appointment is already booked. Redirecting to your upcoming appointments.'); } catch {}
                router.push('/protected/patient?tab=upcoming');
                return;
              }
              try { window.alert(msg); } catch {}
            } finally {
              setBookingInProgress(false);
            }
          }}
        >
          {bookingInProgress ? 'Booking...' : 'Book Appointment'}
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
