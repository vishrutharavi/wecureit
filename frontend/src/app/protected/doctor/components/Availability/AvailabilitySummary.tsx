"use client";

import React from "react";
import styles from "../../doctor.module.scss";

type Item = {
  id: string;
  date: string;
  facilityId: string | null;
  facilityName: string;
  start: string;
  end: string;
  hours: number;
};

type Props = {
  pending: Item[];
  saved: Item[];
  onSaveSchedule: () => void;
  onRemovePending: (id: string) => void;
  onViewSaved: () => void;
  showSavedInline?: boolean;
};

export default function AvailabilitySummary({ pending, saved, onSaveSchedule, onRemovePending, onViewSaved, showSavedInline = true }: Props) {
  return (
    <div style={{ marginTop: 16 }}>
      {pending.length > 0 && (
        <div className={styles.scheduleContainer} style={{ marginBottom: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div>
              <h3 style={{ margin: 0 }}>Your Availability Summary</h3>
              <div style={{ color: '#6b7280', fontSize: 13 }}>{pending.length} day(s) scheduled</div>
            </div>
              <div>
              <button className={styles.viewAppointmentsBtn} onClick={onSaveSchedule}>Save Schedule</button>
            </div>
          </div>

          <div style={{ marginTop: 16 }}>
            {pending.map((p) => (
              <div key={p.id} className={styles.appointmentItem} style={{ marginBottom: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <div style={{ fontWeight: 800, color: '#111' }}>{new Date(p.date).toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' })}</div>
                    <div style={{ color: '#6b7280', fontSize: 13 }}>{new Date(p.date).toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}</div>

                    <div style={{ marginTop: 10 }}>
                      <span className={styles.badge}> {p.facilityName} </span>
                      <div style={{ marginTop: 8, color: '#111', fontWeight: 700 }}>{p.facilityName}</div>
                      <div style={{ color: '#6b7280', fontSize: 13 }}>{p.start} - {p.end}</div>
                    </div>
                  </div>

                  <div style={{ textAlign: 'right' }}>
                    <div style={{ color: '#6b7280', fontSize: 13 }}>{p.hours} hours</div>
                    <div style={{ marginTop: 24 }}>
                      <button className={styles.secondaryBtn} onClick={() => onRemovePending(p.id)}>Remove</button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {showSavedInline !== false && saved.length > 0 && (
        <div className={styles.scheduleContainer} style={{ marginBottom: 12 }}>
          <h3 style={{ marginTop: 0 }}>Saved Availabilities</h3>
          <div style={{ marginTop: 12, display: 'grid', gap: 10 }}>
            {saved.map((sItem) => (
              <div key={sItem.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: 12, borderRadius: 8, background: '#fff' }}>
                <div>
                  <div style={{ fontWeight: 800 }}>{new Date(sItem.date).toLocaleDateString(undefined, { weekday: 'long', month: 'short', day: 'numeric' })}</div>
                  <div style={{ color: '#6b7280' }}>{sItem.start} - {sItem.end}</div>
                </div>
                <div>
                  <button className={styles.viewAppointmentsBtn} onClick={onViewSaved}>View availability</button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
