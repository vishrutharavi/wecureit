"use client";

import React, { useEffect, useState } from "react";
import styles from "../../patient.module.scss";
import { useRouter } from "next/navigation";
import { AiOutlineCalendar } from "react-icons/ai";
import { FiAlertTriangle } from "react-icons/fi";
import { getPatientMe } from '@/lib/auth/authApi';
import { getUpcomingAppointments, cancelAppointment } from '@/lib/patient/patientApi';

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

type AppointmentResp = {
  id?: number | string;
  startTime?: string;
  endTime?: string;
  specialityId?: string | null;
  specialityName?: string | null;
  facilityName?: string | null;
  roomNumber?: string | null;
  doctorName?: string | null;
  isActive?: boolean | null;
};

function formatDoctorName(name?: string | null) {
  if (!name) return '';
  const trimmed = name.trim();
  // if name already starts with Dr or Dr., don't add again
  if (/^dr\.?\s+/i.test(trimmed)) return trimmed;
  return `Dr. ${trimmed}`;
}

function AppointmentCard({ a }: { a: Appointment }) {
  return (
    <div className={styles.appointmentCard}>
      <div className={styles.appointmentCardHeader}>
        <div>
          <div className={styles.dateLabel}>{a.dateLabel}</div>
          <div className={styles.timeLabel}>{a.time}</div>
          {a.doctor ? <div className={styles.doctorName}>{formatDoctorName(a.doctor)}</div> : null}
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
          onClick={async () => {
            // confirm then call backend to cancel
            if (!confirm('Are you sure you want to cancel this appointment?')) return;
            try {
              // ensure we have a token to send — apiFetch will try to refresh, but pass stored token when available
              let token: string | null = null;
              try {
                token = localStorage.getItem('patientToken') ?? localStorage.getItem('idToken') ?? null;
              } catch {}
              if (!token) {
                // no token, ask user to login
                try { window.alert('You must be logged in to cancel appointments. Redirecting to login.'); } catch {}
                window.location.href = '/public/patient/login';
                return;
              }

              await cancelAppointment(a.id, token);
              // remove from UI optimistically by dispatching window event so parent can update state
              window.dispatchEvent(new CustomEvent('wecureit:appointmentCancelled', { detail: { id: a.id } }));
            } catch {
              // apiFetch will show a toast; nothing else needed here
            }
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
            const me = await getPatientMe();
            if (me && me.id) {
              patientId = String(me.id);
              profile = { ...(profile || {}), id: patientId, name: profile?.name ?? me?.name };
              try { localStorage.setItem('patientProfile', JSON.stringify(profile)); } catch {}
            }
          } catch {
            // cannot resolve patient id, bail out
          }
        }

        if (!patientId) return;

        const res = await getUpcomingAppointments(patientId);
        if (!Array.isArray(res)) return;

        const now = new Date();
        const mapped: Appointment[] = res
          .filter((r: AppointmentResp) => (r.isActive === undefined || r.isActive === null) ? true : Boolean(r.isActive))
          .map((r: AppointmentResp) => {
            const start = r?.startTime ? new Date(r.startTime as string) : null;
            const end = r?.endTime ? new Date(r.endTime as string) : null;
            if (!start || !end) return null;
            return {
              id: String(r.id),
              dateLabel: start.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric', year: 'numeric' }),
              dateISO: start.toISOString(),
              time: `${start.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })} - ${end.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}`,
              speciality: r.specialityName ?? r.specialityId ?? null,
              facility: r.facilityName ?? null,
              doctor: r.doctorName ?? null,
              roomNumber: r.roomNumber ?? null,
            } as Appointment;
          })
          .filter((x: Appointment | null) => x !== null)
          .map((x: Appointment) => x)
          .filter((x: Appointment) => new Date(x.dateISO) >= now)
          .sort((a, b) => new Date(a.dateISO).getTime() - new Date(b.dateISO).getTime());

        setAppointments(mapped);
      } catch {
        // ignore fetch errors for now (apiFetch shows a toast)
      }
    }
    load();
  }, []);

  const sorted = React.useMemo(() => appointments, [appointments]);

  // Listen for cancellation events dispatched by AppointmentCard
  React.useEffect(() => {
    function onCancelled(e: Event) {
      try {
        const id = (e as CustomEvent).detail?.id;
        if (!id) return;
        setAppointments((prev) => prev.filter((p) => p.id !== String(id)));
      } catch {}
    }
    window.addEventListener('wecureit:appointmentCancelled', onCancelled as EventListener);
    return () => window.removeEventListener('wecureit:appointmentCancelled', onCancelled as EventListener);
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
