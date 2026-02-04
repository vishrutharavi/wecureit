"use client";

import React from "react";
import styles from "../../patient.module.scss";

type Props = {
  value?: string | null; // YYYY-MM-DD or null/undefined when nothing selected
  onChange: (isoDate: string) => void;
  // if provided, only these dates will be marked available; other future dates will be disabled
  availableDates?: string[] | null;
};

function startOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

function endOfMonth(date: Date) {
  return new Date(date.getFullYear(), date.getMonth() + 1, 0);
}

export default function DatePickerGrid({ value, onChange, availableDates }: Props) {
  const [current, setCurrent] = React.useState<Date>(() => (value ? new Date(value) : new Date()));

  // debug: log availableDates vs the month cells to help diagnose off-by-one
  React.useEffect(() => {
    try {
      const monthIsos = [] as (string | null)[];
      const lastDay = endOfMonth(current);
      const daysInMonth = lastDay.getDate();
      for (let d = 1; d <= daysInMonth; d++) {
        const dt = new Date(current.getFullYear(), current.getMonth(), d);
        const iso = `${dt.getFullYear()}-${String(dt.getMonth() + 1).padStart(2, '0')}-${String(dt.getDate()).padStart(2, '0')}`;
        monthIsos.push(iso);
      }
      console.debug('[calendar] monthIsos', monthIsos);
      console.debug('[calendar] availableDates', availableDates);
    } catch {
      // ignore
    }
  }, [availableDates, current]);

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
          // construct ISO date string (YYYY-MM-DD) using local date parts to avoid timezone shifts
          const iso = `${c.date.getFullYear()}-${String(c.date.getMonth() + 1).padStart(2, '0')}-${String(c.date.getDate()).padStart(2, '0')}`;
          const isSelected = iso === isoSelected;

          // treat date as available if availableDates is null (not fetched) or includes this date
          const isAvailable = typeof availableDates === 'undefined' || availableDates === null ? true : (availableDates.indexOf(iso) >= 0);

          // compute if this date is strictly before today (local)
          const today = new Date();
          const todayMid = new Date(today.getFullYear(), today.getMonth(), today.getDate());
          const cellMid = new Date(c.date.getFullYear(), c.date.getMonth(), c.date.getDate());
          const isPast = cellMid < todayMid;

          const className = isSelected
            ? styles.miniCalendarDaySelected
            : isPast || !isAvailable
            ? styles.miniCalendarDayDisabled
            : styles.miniCalendarDay;

          return (
            <button
              key={idx}
              onClick={() => !isPast && isAvailable && onChange(iso)}
              className={className}
              disabled={isPast || !isAvailable}
              aria-disabled={isPast || !isAvailable}
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
