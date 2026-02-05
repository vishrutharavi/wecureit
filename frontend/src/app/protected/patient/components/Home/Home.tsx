"use client";

import React, { useEffect, useState } from "react";
import styles from "../../patient.module.scss";
import { useRouter } from "next/navigation";
import { AiOutlineCalendar } from "react-icons/ai";
import { FiAlertTriangle } from "react-icons/fi";
import { apiFetch } from '@/lib/api';

type Appointment = {
  id: string;
  dateLabel: string;
  dateISO: string;
  time: string;
  doctor?: string;
  facility?: string;
  speciality?: string | null;
  roomNumber?: string | null;
};

function AppointmentCard({ a }: { a: Appointment }) {
  return (
    <div className={styles.appointmentCard}>
      <div className={styles.appointmentCardHeader}>
        <div>
          <div className={styles.dateLabel}>{a.dateLabel}</div>
          <div className={styles.timeLabel}>{a.time}</div>
          {a.doctor ? <div className={styles.doctorName}>{a.doctor}</div> : null}
          {a.facility ? <div className={styles.facilityName}>{a.facility}</div> : null}
          {a.roomNumber ? <div className={styles.facilityName}>Room: {a.roomNumber}</div> : null}
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
  const [appointments, setAppointments] = useState<Appointment[]>([]);

  useEffect(() => {
    async function load() {
      try {
        const raw = typeof window !== 'undefined' ? localStorage.getItem('patientProfile') : null;
        let profile = raw ? JSON.parse(raw) : null;
        let patientId: string | undefined = profile?.id;
        if (!patientId) {
          try {
            const me = await apiFetch('/api/patient/me');
            if (me && me.id) {
              patientId = String(me.id);
              profile = { ...(profile || {}), id: patientId, name: profile?.name ?? me?.name };
              try { localStorage.setItem('patientProfile', JSON.stringify(profile)); } catch {}
            }
          } catch (e) {
            // cannot resolve patient id, bail out
          }
        }

        if (!patientId) return;

        const res = await apiFetch(`/appointments/byPatient?patientId=${patientId}`);
        if (!Array.isArray(res)) return;

        const now = new Date();
        const mapped: Appointment[] = res
          .map((r: any) => {
            const start = r?.startTime ? new Date(r.startTime) : null;
            const end = r?.endTime ? new Date(r.endTime) : null;
            if (!start || !end) return null;
            return {
              id: String(r.id),
              dateLabel: start.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }),
              dateISO: start.toISOString(),
              time: `${start.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} - ${end.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}`,
              speciality: r.specialityId ?? null,
              roomNumber: r.roomNumber ?? null,
            } as Appointment;
          })
          .filter((x: Appointment | null) => x !== null)
          .map((x: Appointment) => x)
          .filter((x: Appointment) => new Date(x.dateISO) >= now)
          .sort((a, b) => new Date(a.dateISO).getTime() - new Date(b.dateISO).getTime());

        setAppointments(mapped);
      } catch (e) {
        // ignore fetch errors for now (apiFetch shows a toast)
      }
    }
    load();
  }, []);

  const sorted = React.useMemo(() => appointments, [appointments]);

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
          {sorted.length === 0 ? (
            <div className={styles.emptyState}>You have no upcoming appointments.</div>
          ) : (
            sorted.map((a) => <AppointmentCard key={a.id} a={a} />)
          )}
        </div>
      </div>
    </div>
  );
}
