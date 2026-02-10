"use client";
import React, { useState } from "react";
import { apiFetch } from "../../../../../lib/api";
import { FiX } from "react-icons/fi";
import styles from "../../doctor.module.scss";
import AddNoteModal from "../Notes/AddNoteModal";
import type { Appointment } from "./useSchedule";

export default function AppointmentModal({
  open,
  onClose,
  appointments,
}: {
  open: boolean;
  onClose: () => void;
  appointments?: Appointment[];
}) {
  // initial appointments: use passed-in appointments or empty
  const [filter, setFilter] = useState<'UPCOMING' | 'CANCELLED'>('UPCOMING');
  const [items, setItems] = useState<Appointment[]>(appointments ?? []);
  const [completedMenuId, setCompletedMenuId] = useState<string | null>(null);
  const [openAddNoteId, setOpenAddNoteId] = useState<string | null>(null);
  const [completedIds, setCompletedIds] = useState<Record<string, boolean>>({});
  const [noteAppointment, setNoteAppointment] = useState<Appointment | null>(null);
  
  // keep items in sync when parent passes new appointments
  React.useEffect(() => {
    setItems(appointments ?? []);
    setFilter('UPCOMING');
    setCompletedIds({});
  }, [appointments]);

  if (!open) return null;

  const upcoming = items.filter((a) => a.status === "UPCOMING");
  const cancelled = items.filter((a) => a.status === "CANCELLED");
  const selectedAddNoteAppointment = noteAppointment ?? items.find(it => it.id === openAddNoteId) ?? null;

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
                        <div style={{ fontSize: 13, color: '#666', marginTop: 6 }}>{formatHourRange(a.start, a.end)}</div>
                        {a.notes && (
                          <div style={{ marginTop: 10 }}>
                            <div style={{ fontSize: 12, color: '#9b9b9b', marginBottom: 6, fontWeight: 700 }}>Chief complaints</div>
                            <div style={{ background: '#fff', padding: '10px', borderRadius: 8, color: '#5b5b5b', fontSize: 13, border: '1px solid rgba(0,0,0,0.04)' }}>{a.notes}</div>
                          </div>
                        )}
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
                            <button className={`${styles.completedPrimaryBtn}`} onClick={async () => {
                              // Mark appointment completed on server, then open add-note modal
                              try {
                                const raw = localStorage.getItem('doctorProfile');
                                if (!raw) throw new Error('Doctor profile not found');
                                const doc = JSON.parse(raw);
                                const doctorId = doc.id;
                                const token = localStorage.getItem('doctorToken') ?? undefined;
                                // appointment id expected as numeric id in backend
                                await apiFetch(`/api/doctors/${doctorId}/appointments/${a.id}/complete`, token, { method: 'POST' });
                                // persist appointment object for the add-note modal (we remove it from list)
                                setNoteAppointment(a);
                                // remove from current list so it no longer appears in Upcoming
                                setItems(prev => prev.filter(it => it.id !== a.id));
                                // mark completed locally and open add-note modal
                                setOpenAddNoteId(a.id);
                                setCompletedIds(prev => ({ ...prev, [a.id]: true }));
                                setCompletedMenuId(null);
                              } catch (err) {
                                console.error('Complete appointment failed', err);
                                setCompletedMenuId(null);
                              }
                            }}>Add a note</button>
                            <button className={`${styles.completedSecondaryBtn}`} onClick={async () => {
                              // mark completed without adding a note
                              try {
                                const raw = localStorage.getItem('doctorProfile');
                                if (!raw) throw new Error('Doctor profile not found');
                                const doc = JSON.parse(raw);
                                const doctorId = doc.id;
                                const token = localStorage.getItem('doctorToken') ?? undefined;
                                await apiFetch(`/api/doctors/${doctorId}/appointments/${a.id}/complete`, token, { method: 'POST' });
                                setItems(prev => prev.filter(it => it.id !== a.id));
                                setCompletedIds(prev => ({ ...prev, [a.id]: true }));
                                setCompletedMenuId(null);
                              } catch (err) {
                                console.error('Complete appointment failed', err);
                                setCompletedMenuId(null);
                              }
                            }}>Add later</button>
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
                        <div style={{ fontSize: 13, color: '#666', marginTop: 6 }}>{formatHourRange(a.start, a.end)}</div>
                        {a.notes && (
                          <div style={{ marginTop: 10 }}>
                            <div style={{ fontSize: 12, color: '#9b9b9b', marginBottom: 6, fontWeight: 700 }}>Chief complaints</div>
                            <div style={{ background: '#fff', padding: '10px', borderRadius: 8, color: '#5b5b5b', fontSize: 13, border: '1px solid rgba(0,0,0,0.04)' }}>{a.notes}</div>
                          </div>
                        )}
                      </div>
                      <div className={styles.appointmentMeta}>
                        {/* If backend marked cancelledBy as 'patient', show Cancelled by patient name */}
                        {a.cancelledBy && a.cancelledBy.toLowerCase() === 'patient' ? (
                          <div style={{ fontSize: 12, color: '#6b6b6b', textAlign: 'right' }}>
                            Cancelled by {a.patientName}
                          </div>
                        ) : (
                          <div style={{ fontSize: 12, color: '#6b6b6b', textAlign: 'right' }}>Cancelled</div>
                        )}
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
            onClose={() => { setOpenAddNoteId(null); setNoteAppointment(null); }}
            patientName={selectedAddNoteAppointment.patientName}
            dateLabel={formatHourRange(selectedAddNoteAppointment.start, selectedAddNoteAppointment.end)}
            timeLabel={getDurationLabel(selectedAddNoteAppointment.start, selectedAddNoteAppointment.end)}
            appointmentDbId={selectedAddNoteAppointment.id}
            patientId={selectedAddNoteAppointment.patientId}
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
