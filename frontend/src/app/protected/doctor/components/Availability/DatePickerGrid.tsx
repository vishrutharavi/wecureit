"use client";

import styles from "../../doctor.module.scss";
import React from "react";

type Props = {
  selectedDate: string;
  setSelectedDate: (d: string) => void;
  weekIndex: number;
  setWeekIndex: (i: number) => void;
};

export default function DatePickerGrid({ selectedDate, setSelectedDate, weekIndex, setWeekIndex }: Props) {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <div style={{ color: '#666' }}>Choose a date to set availability</div>
        <div className={styles.weekSwitch}>
          <button className={`${styles.weekBtn} ${weekIndex === 1 ? ' ' + styles.active : ''}`} onClick={() => setWeekIndex(1)}>‹ Week 1</button>
          <button className={`${styles.weekBtn} ${weekIndex === 2 ? ' ' + styles.active : ''}`} onClick={() => setWeekIndex(2)}>Week 2 ›</button>
        </div>
      </div>

      <div className={styles.dateGrid} style={{ gridTemplateColumns: 'repeat(7, 1fr)' }}>
        {Array.from({ length: 14 }).slice((weekIndex-1)*7, weekIndex*7).map((_, idx) => {
          const base = new Date();
          const d = new Date(base);
          d.setDate(base.getDate() + ((weekIndex-1)*7 + idx));
          const iso = d.toISOString().slice(0,10);
          const day = d.getDate();
          const month = d.toLocaleString(undefined, { month: 'short' });
          const weekday = d.toLocaleDateString(undefined, { weekday: 'short' });
          return (
            <button
              key={iso}
              className={`${styles.dateCard} ${selectedDate === iso ? styles.selected : ''}`}
              onClick={() => setSelectedDate(iso)}
            >
              <div className={styles.cardTitle} style={{ fontSize: 13 }}>{weekday}</div>
              <div style={{ fontSize: 28, fontWeight: 800, margin: '8px 0' }}>{day}</div>
              <div className={styles.cardDate} style={{ marginBottom: 0 }}>{month}</div>
            </button>
          );
        })}
      </div>
    </div>
  );
}
