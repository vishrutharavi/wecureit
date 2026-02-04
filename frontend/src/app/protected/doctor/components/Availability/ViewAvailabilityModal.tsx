"use client";

import React from "react";
import styles from "../../doctor.module.scss";
import { FiX } from "react-icons/fi";

type AvailabilityItem = {
  id: string;
  date: string;
  facilityName: string;
  facilityCity?: string;
  specialty?: string;
  start: string;
  end: string;
  hours: number;
  allowWalkIn?: boolean;
  isBookable?: boolean;
  specialities?: string[];
  roomsCount?: number;
  facilityAddress?: string;
  facilityState?: string;
  assigned?: boolean;
};

type Props = {
  open: boolean;
  onClose: () => void;
  items: AvailabilityItem[];
  onRemove?: (id: string) => void;
  onToggleWalkIn?: (id: string, allow: boolean) => Promise<void>;
};

export default function ViewAvailabilityModal({ open, onClose, items, onRemove, onToggleWalkIn }: Props) {
  const [selected, setSelected] = React.useState<AvailabilityItem | null>(null);

  if (!open) return null;

  // determine today's ISO date in local timezone (YYYY-MM-DD)
  const getTodayIso = () => {
    const d = new Date();
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  const parseHM = (s: string | undefined) => {
    if (!s) return null;
    const m = s.match(/^(\d{1,2}):(\d{2})$/);
    if (!m) return null;
    const hh = parseInt(m[1], 10);
    const mm = parseInt(m[2], 10);
    return hh * 60 + mm;
  };

  const nowMinutes = (() => {
    const d = new Date();
    return d.getHours() * 60 + d.getMinutes();
  })();

  // Filter assigned items. Also exclude today's availabilities that have already finished
  // according to local time (i.e. end time <= now). This prevents showing past availabilities
  // for today in the modal.
  const assignedItems = items
    .filter(it => it.assigned)
    .filter((it) => {
      try {
        const today = getTodayIso();
        if (String(it.date) !== today) return true;
        const endM = parseHM(it.end);
        if (endM == null) return true; // can't determine — keep it
        return endM > nowMinutes; // keep only if end time is in the future
      } catch {
        return true;
      }
    });

  const handleView = (it: AvailabilityItem) => {
    setSelected(it);
  };

  const handleRemove = (id: string) => {
    if (onRemove) onRemove(id);
    // close detail view after removal
    setSelected(null);
  };

  const handleToggle = async (it: AvailabilityItem) => {
    if (!onToggleWalkIn) return;
    const newVal = !it.allowWalkIn;
    try {
      await onToggleWalkIn(it.id, newVal);
      // reflect change in detail view if open
      setSelected(prev => prev && prev.id === it.id ? { ...prev, allowWalkIn: newVal, isBookable: newVal ? false : prev.isBookable } : prev);
    } catch (err) {
      console.error('Failed toggle walk-in', err);
      // optionally show UI feedback here
    }
  };

  return (
    <div className={styles["modal-overlay"]}>
      <div className={styles["modal-container"]}>
        <div className={styles["modal-header"]}>
          <div className={styles["modal-title"]}>{selected ? 'Availability' : 'Saved Availabilities'}</div>
          <button onClick={onClose} aria-label="Close" style={{ background: 'transparent', border: 'none', cursor: 'pointer' }}>
            <FiX size={22} />
          </button>
        </div>

        <div className={styles["modal-body"]}>
          {!selected ? (
            assignedItems.length === 0 ? (
              <div className={styles.emptyCard}>No assigned availabilities</div>
            ) : (
              <div style={{ display: 'grid', gap: 10 }}>
                {assignedItems.map((it) => (
                  <div key={it.id} style={{ padding: 12, borderRadius: 8, background: '#fff', border: '1px solid rgba(254,202,202,0.6)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <div style={{ fontWeight: 800 }}>{new Date(it.date).toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })}</div>
                        <div style={{ color: '#6b7280', display: 'flex', gap: 8, alignItems: 'center' }}>
                          <span>{it.facilityName}</span>
                          {it.allowWalkIn ? <span className={styles.badge} style={{ background: 'rgba(250,204,21,0.12)', color: '#b45309' }}>Walk-in only</span> : null}
                          {/* If server says not bookable but roomsCount > 0, prefer showing as bookable (server may not have returned roomsCount); only show Not bookable when explicitly not bookable and no rooms available */}
                          {it.isBookable === false && !it.allowWalkIn && ((it.roomsCount ?? 0) === 0) ? <span className={styles.badge} style={{ background: 'rgba(239,68,68,0.08)', color: 'var(--doctor-dark)' }}>Not bookable</span> : null}
                        </div>
                        <div style={{ marginTop: 6 }}>{it.start} - {it.end} • {it.hours} hrs</div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <button className={styles.viewAppointmentsBtn} onClick={() => handleView(it)}>View</button>
                      <button className={styles.walkInToggleBtn} onClick={() => handleToggle(it)}>
                        {it.allowWalkIn ? 'Unmark Walk-in' : 'Mark Walk-in'}
                      </button>
                      {onRemove ? (
                        <button className={styles.secondaryBtn} onClick={() => { if (confirm('Delete this availability? This cannot be undone.')) { handleRemove(it.id); } }} style={{ marginLeft: 8 }}>Delete</button>
                      ) : null}
                    </div>
                  </div>
                ))}
              </div>
            )
          ) : (
            // detail view
            <div className={styles.appointmentCard} style={{ border: '1px solid rgba(239,68,68,0.12)', background: 'linear-gradient(180deg, #fff7f7, #fff)', padding: 18 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div>
                  <div style={{ fontWeight: 800, fontSize: 18 }}>{new Date(selected.date).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}</div>
                  <div style={{ color: '#6b7280', marginTop: 4 }}>{new Date(selected.date).toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}</div>

                    <div style={{ marginTop: 12 }}>
                      <span className={styles.badge} style={{ background: 'rgba(239,68,68,0.08)', color: 'var(--doctor-dark)' }}>{selected.specialities && selected.specialities.length > 0 ? selected.specialities[0] : (selected.specialty ?? 'General')}</span>

                    {selected.allowWalkIn ? (
                      <div style={{ marginTop: 8 }}><span className={styles.badge} style={{ background: 'rgba(250,204,21,0.12)', color: '#b45309' }}>Walk-in only</span></div>
                    ) : (selected.isBookable === false && ((selected.roomsCount ?? 0) === 0)) ? (
                      <div style={{ marginTop: 8 }}><span className={styles.badge} style={{ background: 'rgba(239,68,68,0.08)', color: 'var(--doctor-dark)' }}>Not bookable</span></div>
                    ) : null}

                    <div style={{ marginTop: 10, fontWeight: 700 }}>{selected.facilityName}</div>
                      {selected.facilityAddress && <div style={{ color: '#6b7280', fontSize: 13 }}>{selected.facilityAddress}{selected.facilityState ? `, ${selected.facilityState}` : ''}</div>}

                    <div style={{ marginTop: 12, display: 'flex', gap: 12, alignItems: 'center' }}>
                      <div style={{ display: 'flex', flexDirection: 'column' }}>
                        <div style={{ fontWeight: 800 }}>{selected.start} - {selected.end}</div>
                        <div style={{ color: '#6b7280', fontSize: 13 }}>{selected.hours} hours</div>
                      </div>
                    </div>
                  </div>
                </div>

                <div style={{ textAlign: 'right' }}>
                  <button onClick={() => setSelected(null)} className={styles.secondaryBtn} style={{ marginBottom: 12 }}>Back</button>
                  <div>
                    <button onClick={() => handleRemove(selected.id)} style={{ background: 'transparent', border: 'none', color: 'var(--doctor-primary)', fontWeight: 700, cursor: 'pointer' }}>Remove</button>
                    <div style={{ marginTop: 10 }}>
                      <button className={styles.walkInToggleBtn} onClick={() => selected && handleToggle(selected)}>
                        {selected?.allowWalkIn ? 'Unmark Walk-in' : 'Mark Walk-in'}
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
          <button className={styles.secondaryBtn} onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}
