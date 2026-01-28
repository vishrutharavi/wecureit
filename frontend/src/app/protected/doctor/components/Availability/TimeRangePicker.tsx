"use client";

import React from "react";
import styles from "../../doctor.module.scss";

type Props = {
  startTime: string;
  endTime: string;
  setStartTime: (s: string) => void;
  setEndTime: (e: string) => void;
};

const times = Array.from({ length: 24 }).map((_, i) => `${i.toString().padStart(2, '0')}:00`);

export default function TimeRangePicker({ startTime, endTime, setStartTime, setEndTime }: Props) {
  const startIdx = times.indexOf(startTime);
  const endIdx = times.indexOf(endTime);
  const total = startIdx >= 0 && endIdx >= 0 ? Math.max(0, endIdx - startIdx) : 0;

  return (
    <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
      <div style={{ flex: 1 }}>
        <label style={{ fontSize: 16, fontWeight: 800, color: 'var(--doctor-dark)', display: 'block', marginBottom: 8 }}>Start</label>
        <select className={styles.timeSelect} value={startTime} onChange={(e) => setStartTime(e.target.value)}>
          <option value="">Select start</option>
          {times.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
      </div>

      <div style={{ flex: 1 }}>
        <label style={{ fontSize: 16, fontWeight: 800, color: 'var(--doctor-dark)', display: 'block', marginBottom: 8 }}>End</label>
        <select className={styles.timeSelect} value={endTime} onChange={(e) => setEndTime(e.target.value)}>
          <option value="">Select end</option>
          {times.map(t => <option key={t} value={t}>{t}</option>)}
        </select>
      </div>

      <div style={{ minWidth: 120, textAlign: 'center' }}>
        <div style={{ fontSize: 12, color: '#666' }}>Total</div>
        <div style={{ fontWeight: 700 }}>{total} hrs</div>
      </div>
    </div>
  );
}
