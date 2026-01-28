"use client";
import React, { useState } from "react";
import { FiX } from "react-icons/fi";
import styles from "../../doctor.module.scss";
import AddNoteModal from "../Notes/AddNoteModal";

type Appointment = {
  id: string;
  patientName: string;
  start: string; // e.g. "2024-01-28T10:00:00"
  end: string;
  status: "UPCOMING" | "CANCELLED" | "COMPLETED";
};

export default function AppointmentModal({
  open,
  onClose,
  appointments,
}: {
  open: boolean;
  onClose: () => void;
  appointments?: Appointment[];
}) {
  // initial appointments: either passed in or sample placeholders
  const initialAppointments: Appointment[] = appointments ?? [
    { id: "1", patientName: "Michael Brown", start: "2026-01-28T10:00:00", end: "2026-01-28T10:30:00", status: "UPCOMING" },
    { id: "2", patientName: "Sara Lee", start: "2026-01-28T11:00:00", end: "2026-01-28T11:30:00", status: "CANCELLED" },
    { id: "3", patientName: "James Smith", start: "2026-01-29T09:00:00", end: "2026-01-29T09:30:00", status: "UPCOMING" },
  ];

  const [filter, setFilter] = useState<'UPCOMING' | 'CANCELLED' | 'ALL'>('UPCOMING');
  const [items, setItems] = useState<Appointment[]>(initialAppointments);
  const [completedMenuId, setCompletedMenuId] = useState<string | null>(null);
  const [openAddNoteId, setOpenAddNoteId] = useState<string | null>(null);
  const [completedIds, setCompletedIds] = useState<Record<string, boolean>>({});
  if (!open) return null;

  const upcoming = items.filter((a) => a.status === "UPCOMING");
  const cancelled = items.filter((a) => a.status === "CANCELLED");
  const selectedAddNoteAppointment = items.find(it => it.id === openAddNoteId) || null;

  return (
    <div className={styles['modal-overlay']} onClick={onClose}>
      <div className={styles['modal-container']} onClick={(e) => e.stopPropagation()} style={{ maxWidth: 720 }}>
        <div className={styles['modal-header']}>
          <h2 className={styles['modal-title']}>Appointments</h2>
          <button onClick={onClose} aria-label="Close"><FiX size={18} /></button>
        </div>

        <div className={styles['modal-body']}>
          <div className={styles.modalFilterBtns}>
            <button
              className={`${styles.modalFilterBtn} ${filter === 'UPCOMING' ? styles.active : ''}`}
              onClick={() => setFilter('UPCOMING')}
            >
              Upcoming
            </button>
            <button
              className={`${styles.modalFilterBtn} ${filter === 'CANCELLED' ? styles.active : ''}`}
              onClick={() => setFilter('CANCELLED')}
            >
              Cancelled
            </button>
          </div>

          <section className={styles['modal-section']}>
            {filter === 'UPCOMING' && (
              <>
                {upcoming.length ? (
                  upcoming.map((a) => (
                    <div key={a.id} className={styles.appointmentRow} style={{ padding: '8px 0', borderBottom: '1px solid #eee' }}>
                      <div className={styles.appointmentInfo}>
                        <div><span className={styles.appointmentPatientName}>{a.patientName}</span></div>
                        <div style={{ fontSize: 13, color: '#666' }}>{formatHourRange(a.start, a.end)}</div>
                      </div>
                      <div className={styles.appointmentMeta}>
                        <span className={styles.appointmentDurationPill}>{getDurationLabel(a.start, a.end)}</span>
                        <div className={styles.appointmentActionBtns}>
                          <button
                            className={`${styles.appointmentActionBtn} ${styles.cancel}`}
                            onClick={() => {
                              setItems(prev => prev.map(it => it.id === a.id ? { ...it, status: 'CANCELLED' } : it));
                              setFilter('CANCELLED');
                            }}
                          >
                            Cancel
                          </button>
                          <button className={`${styles.completedPrimaryBtn}`} onClick={() => setCompletedMenuId(a.id)}>Completed</button>
                        </div>
                        {completedMenuId === a.id && (
                          <div className={styles.completedMenu}>
                 <button className={`${styles.completedPrimaryBtn}`} onClick={() => { setOpenAddNoteId(a.id); setCompletedIds(prev => ({ ...prev, [a.id]: true })); setCompletedMenuId(null); }}>Add a note</button>
                 <button className={`${styles.completedSecondaryBtn}`} onClick={() => { setCompletedIds(prev => ({ ...prev, [a.id]: true })); setCompletedMenuId(null); }}>Add later</button>
                          </div>
                        )}
                        {completedIds[a.id] && !openAddNoteId && <div style={{ fontSize: 12, color: '#6b6b6b' }}>Completed</div>}
                      </div>
                      {/* inline editor removed in favor of modal */}
                    </div>
                  ))
                ) : (
                  <div className={styles.emptyCard}>No upcoming appointments</div>
                )}
              </>
            )}

            {filter === 'CANCELLED' && (
              <>
                {cancelled.length ? (
                  cancelled.map((a) => (
                    <div key={a.id} className={styles.appointmentRow} style={{ padding: '8px 0', borderBottom: '1px solid #eee' }}>
                      <div className={styles.appointmentInfo}>
                        <div><span className={styles.appointmentPatientNameCancelled}>{a.patientName}</span></div>
                        <div style={{ fontSize: 13, color: '#666' }}>{formatHourRange(a.start, a.end)}</div>
                      </div>
                      <div className={styles.appointmentMeta}>
                        <div style={{ fontSize: 12, color: '#6b6b6b', textAlign: 'right' }}>Cancelled</div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className={styles.emptyCard}>No cancelled appointments</div>
                )}
              </>
            )}
          </section>
        </div>
        {selectedAddNoteAppointment && (
          <AddNoteModal
            open={!!openAddNoteId}
            onClose={() => setOpenAddNoteId(null)}
            patientName={selectedAddNoteAppointment.patientName}
            dateLabel={formatHourRange(selectedAddNoteAppointment.start, selectedAddNoteAppointment.end)}
            timeLabel={getDurationLabel(selectedAddNoteAppointment.start, selectedAddNoteAppointment.end)}
          />
        )}

        <div className={styles['modal-footer']} style={{ display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
          <button className={styles.secondaryBtn} onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}

function formatHourRange(startIso: string, endIso: string) {
  const s = new Date(startIso);
  const e = new Date(endIso);
  return `${s.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${e.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

function getDurationLabel(startIso: string, endIso: string) {
  const s = new Date(startIso);
  const e = new Date(endIso);
  const mins = Math.round((e.getTime() - s.getTime()) / 60000);
  if (mins >= 60) return `${Math.round(mins / 60)} hr`;
  return `${mins} min`;
}
