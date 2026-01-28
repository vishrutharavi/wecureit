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
};

type Props = {
  open: boolean;
  onClose: () => void;
  items: AvailabilityItem[];
  onRemove?: (id: string) => void;
};

export default function ViewAvailabilityModal({ open, onClose, items, onRemove }: Props) {
  const [selected, setSelected] = React.useState<AvailabilityItem | null>(null);

  if (!open) return null;

  const handleView = (it: AvailabilityItem) => {
    setSelected(it);
  };

  const handleRemove = (id: string) => {
    if (onRemove) onRemove(id);
    // close detail view after removal
    setSelected(null);
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
            items.length === 0 ? (
              <div className={styles.emptyCard}>No saved availabilities</div>
            ) : (
              <div style={{ display: 'grid', gap: 10 }}>
                {items.map((it) => (
                  <div key={it.id} style={{ padding: 12, borderRadius: 8, background: '#fff', border: '1px solid rgba(254,202,202,0.6)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <div>
                      <div style={{ fontWeight: 800 }}>{new Date(it.date).toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })}</div>
                      <div style={{ color: '#6b7280' }}>{it.facilityName}</div>
                      <div style={{ marginTop: 6 }}>{it.start} - {it.end} • {it.hours} hrs</div>
                    </div>
                    <div>
                      <button className={styles.viewAppointmentsBtn} onClick={() => handleView(it)}>View</button>
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
                    <span className={styles.badge} style={{ background: 'rgba(239,68,68,0.08)', color: 'var(--doctor-dark)' }}>{selected.specialty ?? 'General'}</span>

                    <div style={{ marginTop: 10, fontWeight: 700 }}>{selected.facilityName}</div>
                    {selected.facilityCity && <div style={{ color: '#6b7280', fontSize: 13 }}>{selected.facilityCity}</div>}

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
