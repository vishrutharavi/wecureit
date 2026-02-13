"use client";

import React from "react";
import styles from "../../patient.module.scss";
import PaymentSection from "../MyProfile/Payment";
import CostSummary from "./CostSummary";

function ReadField({ children }: { children: React.ReactNode }) {
  return <div className={styles.readField}>{children}</div>;
}

type Booking = {
  doctor?: { id?: string; name?: string } | null;
  facility?: { id?: string; name?: string; address?: string } | null;
  specialty?: { id?: string; name?: string } | null;
  date?: string | null;
  time?: string | null;
  duration?: number | null;
};

export default function Confirmation() {
  const [booking, setBooking] = React.useState<Booking | null>(null);

  React.useEffect(() => {
    try {
      const raw = sessionStorage.getItem("bookingSelection");
      if (raw) setBooking(JSON.parse(raw));
    } catch {}
  }, []);

  const doctor = booking?.doctor;
  const facility = booking?.facility;
  const date = booking?.date;
  const time = booking?.time;
  const duration = booking?.duration;

  return (
    <div className={styles.wrapper}>
      <div className={styles.bookingHeader}>
        <a href="/protected/patient?tab=datetimeselection" className={styles.backLink}>← Back to Date & Time</a>
        <h1 className={styles.bookingTitle}>Confirm Appointment</h1>
        <div className={styles.subtitle}>Review your appointment details and confirm your booking</div>
      </div>

      <div className={styles.bookingGrid}>
        <div>
          <div className={styles.panelWhite}>
            <div className={styles.sectionTitle}>Appointment Details</div>
            <div className={styles.sectionSubtitle}>Please review your appointment information</div>

            <div className={`${styles.summaryCard} ${styles.mt12}`}>
              <div className={styles.fieldLabel}>Doctor</div>
              <ReadField>{doctor?.name || '—'}</ReadField>
            </div>

            <div className={styles.summaryCard}>
              <div className={styles.fieldLabel}>Facility</div>
              <ReadField>{facility?.name || '—'}</ReadField>
              <div className={`${styles.mt6} ${styles.muted}`}>{facility?.address || ''}</div>
            </div>

            <div className={styles['grid-two-col-12']}>
              <div className={styles.summaryCard}>
                <div className={styles.fieldLabel}>Date</div>
                <ReadField>
                  {date ? (() => {
                    // Parse as local date to avoid timezone issues
                    const [year, month, day] = date.split('-').map(Number);
                    const localDate = new Date(year, month - 1, day);
                    return localDate.toLocaleDateString(undefined, { 
                      weekday: 'long', 
                      year: 'numeric', 
                      month: 'long', 
                      day: 'numeric' 
                    });
                  })() : '—'}
                </ReadField>
              </div>

              <div className={styles.summaryCard}>
                <div className={styles.fieldLabel}>Time</div>
                <ReadField>{time ? `${time}
                ` : 'No time selected'}</ReadField>
                <div className={styles['time-note']}>Duration: {duration ? `${duration} minutes` : '—'}</div>
              </div>
            </div>

            <div className={styles.mt18}>
              <div className={styles.sectionTitle}>Payment Method</div>
              <div className={styles.sectionSubtitle}>Payment will be collected at the facility</div>
              <div className={styles.mt12}>
                {/* Reuse the profile payment UI so users can add/delete cards here */}
                <PaymentSection />
              </div>
            </div>
          </div>
        </div>

        <div>
          <CostSummary />
        </div>
      </div>
    </div>
  );
}
