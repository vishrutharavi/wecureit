"use client";

import React from "react";
import styles from "../../patient.module.scss";

type Props = {
  value?: string | null; // YYYY-MM-DD or null/undefined when nothing selected
  onChange: (isoDate: string) => void;
};

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function endOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

export default function DatePickerGrid({ value, onChange }: Props) {
  const [current, setCurrent] = React.useState<Date>(() => (value ? new Date(value) : new Date()));

  React.useEffect(() => {
    setCurrent(value ? new Date(value) : new Date());
  }, [value]);

  const firstDay = startOfMonth(current);
  const lastDay = endOfMonth(current);

  const daysInMonth = lastDay.getDate();
  const startWeekday = firstDay.getDay(); // 0 (Sun) - 6

  const cells = [] as Array<{ date: Date | null }>;
  // pad leading blanks
  for (let i = 0; i < startWeekday; i++) cells.push({ date: null });
  for (let d = 1; d <= daysInMonth; d++) cells.push({ date: new Date(current.getFullYear(), current.getMonth(), d) });

  const isoSelected = value;

  const prevMonth = () => setCurrent((c) => new Date(c.getFullYear(), c.getMonth() - 1, 1));
  const nextMonth = () => setCurrent((c) => new Date(c.getFullYear(), c.getMonth() + 1, 1));

  return (
    <div className={styles.miniCalendarWrap}>
      <div className={styles.miniCalendar}>
      <div className={styles.miniCalendarHeader}>
        <button className={styles.miniCalendarNav} onClick={prevMonth}>‹</button>
        <div>{current.toLocaleString(undefined, { month: 'long', year: 'numeric' })}</div>
        <button className={styles.miniCalendarNav} onClick={nextMonth}>›</button>
      </div>

      <div className={styles.miniCalendarGrid}>
        {['Su','Mo','Tu','We','Th','Fr','Sa'].map((d) => (
          <div key={d} className={styles.miniCalendarWeekday}>{d}</div>
        ))}

        {cells.map((c, idx) => {
          if (!c.date) return <div key={idx} className={styles.miniCalendarCellEmpty} />;
          const iso = c.date.toISOString().slice(0,10);
          const isSelected = iso === isoSelected;

          // compute if this date is strictly before today (local)
          const today = new Date();
          const todayMid = new Date(today.getFullYear(), today.getMonth(), today.getDate());
          const cellMid = new Date(c.date.getFullYear(), c.date.getMonth(), c.date.getDate());
          const isPast = cellMid < todayMid;

          const className = isSelected
            ? styles.miniCalendarDaySelected
            : isPast
            ? styles.miniCalendarDayDisabled
            : styles.miniCalendarDay;

          return (
            <button
              key={idx}
              onClick={() => !isPast && onChange(iso)}
              className={className}
              disabled={isPast}
              aria-disabled={isPast}
            >
              {c.date.getDate()}
            </button>
          );
        })}
      </div>
      </div>
    </div>
  );
}
