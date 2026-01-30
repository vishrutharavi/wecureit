"use client";

import React from "react";
import styles from "../../patient.module.scss";
import { useRouter } from "next/navigation";
import { AiOutlineCalendar } from "react-icons/ai";
import { FiAlertTriangle } from "react-icons/fi";

type Appointment = {
  id: string;
  dateLabel: string;
  dateISO: string;
  time: string;
  doctor: string;
  facility: string;
  speciality: string;
};

const SAMPLE_APPOINTMENTS: Appointment[] = [
  {
    id: "a1",
    dateLabel: "Mon, Oct 27, 2025",
    dateISO: "2025-10-27T10:00:00",
    time: "10:00 AM - 10:30 AM",
    doctor: "Dr. Sarah Johnson",
    facility: "Downtown Medical Center",
    speciality: "Cardiology",
  },
  {
    id: "a2",
    dateLabel: "Tue, Nov 4, 2025",
    dateISO: "2025-11-04T14:00:00",
    time: "2:00 PM - 2:45 PM",
    doctor: "Dr. Michael Chen",
    facility: "Alexandria Main Hospital",
    speciality: "Orthopedics",
  },
];

function AppointmentCard({ a }: { a: Appointment }) {
  return (
    <div className={styles.appointmentCard}>
      <div className={styles.appointmentCardHeader}>
        <div>
          <div className={styles.dateLabel}>{a.dateLabel}</div>
          <div className={styles.timeLabel}>{a.time}</div>
          <div className={styles.doctorName}>{a.doctor}</div>
          <div className={styles.facilityName}>{a.facility}</div>
        </div>

        <div style={{ textAlign: "right" }}>
          <div className={styles.smallBadge}>{a.speciality}</div>
        </div>
      </div>

      <div className={`${styles.infoBox} ${styles.infoBoxSpacing}`}>
        <div className={styles.infoBoxInner}>
          <FiAlertTriangle size={18} style={{ color: '#f59e0b' }} />
          Cancellation fee: $50
        </div>
        <div className={styles.infoBoxNote}>(within 24 hours of appointment)</div>
      </div>

      <div style={{ marginTop: 12 }}>
        <button
          className={styles.cancelBtn}
          onClick={() => {
            // placeholder: action to cancel
            alert("Cancel appointment: " + a.id);
          }}
        >
          ⊗ Cancel Appointment
        </button>
      </div>
    </div>
  );
}

export default function Home() {
  const router = useRouter();
  const sorted = React.useMemo(() => {
    return [...SAMPLE_APPOINTMENTS].sort((a, b) => {
      const ta = new Date(a.dateISO).getTime();
      const tb = new Date(b.dateISO).getTime();
      return tb - ta; // recent to oldest
    });
  }, []);

  return (
    <div className={styles.homeRoot}>
      {/* Book appointment container */}
      <div className={styles.bookWrap}>
        <div className={`${styles.panelWhite} ${styles.bookPanel} ${styles.bookPanelCenter}`}>
          <div className={styles.bookIcon}>
            <AiOutlineCalendar size={56} />
          </div>
          <div className={`${styles.bookTitle} ${styles.bookTitleLarge}`}>Need to see a doctor?</div>
          <div className={`${styles.bookSubtitle} ${styles.bookSubtitleSpacing}`}>Book an appointment with our specialists</div>
          <div>
            <button
              className={`${styles.viewAppointmentsBtn} ${styles.bookButtonLarge}`}
              onClick={() => router.push('/protected/patient?tab=dropdownselection')}
            >
              Book New Appointment
            </button>
          </div>
        </div>
      </div>

      {/* Upcoming appointments container */}
      <div className={`${styles.panelWhite} ${styles.upcomingPanel}`}>
        <div style={{ marginBottom: 12 }}>
          <h3 className={styles.upcomingHeaderTitle}>Upcoming Appointments</h3>
          <div className={styles.upcomingHeaderSubtitle}>Your scheduled visits</div>
        </div>

        <div>
          {sorted.map((a) => (
            <AppointmentCard key={a.id} a={a} />
          ))}
        </div>
      </div>
    </div>
  );
}
