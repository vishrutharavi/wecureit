"use client";

import styles from "../../doctor.module.scss";
import { useState } from "react";
import DatePickerGrid from "./DatePickerGrid";
import FacilitySelector from "./FacilitySelector";
import TimeRangePicker from "./TimeRangePicker";

export default function AvailabilityView() {
  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().slice(0,10));
  const [selectedFacility, setSelectedFacility] = useState<string | null>(null);
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("17:00");
  const [weekIndex, setWeekIndex] = useState<number>(1);

  const parseMinutes = (t: string) => {
    const [h, m] = t.split(':').map((x) => parseInt(x, 10));
    return (isNaN(h) ? 0 : h) * 60 + (isNaN(m) ? 0 : m);
  };

  const totalMinutes = Math.max(0, parseMinutes(endTime) - parseMinutes(startTime));
  const totalHours = +(totalMinutes / 60).toFixed(2);
  const minHoursSatisfied = totalHours >= 4;

  const facilities = [
    { id: "1", name: "Downtown Medical Center", city: "Washington DC", rooms: 12 },
    { id: "2", name: "Alexandria Main Hospital", city: "Alexandria", rooms: 15 },
    { id: "3", name: "Bethesda Health Center", city: "Bethesda", rooms: 9 },
  ];

  return (
    <div className={styles.scheduleContainer}>
      <h2>Set Your Availability</h2>
      <p className={styles.subtitle}>
        Choose date, facility, and working hours. Minimum 4 hours per day.
      </p>

      <section className={styles.section}>
        <h4>1. Select Date</h4>
        <DatePickerGrid
          selectedDate={selectedDate}
          setSelectedDate={setSelectedDate}
          weekIndex={weekIndex}
          setWeekIndex={setWeekIndex}
        />
      </section>

      <section className={styles.section}>
        <h4>2. Select Facility</h4>
        <FacilitySelector
          facilities={facilities}
          selectedFacility={selectedFacility}
          setSelectedFacility={setSelectedFacility}
        />
      </section>

      <section className={styles.section}>
        <h4>3. Select Working Hours (Minimum 4 hours)</h4>
        <TimeRangePicker
          startTime={startTime}
          endTime={endTime}
          setStartTime={setStartTime}
          setEndTime={setEndTime}
        />

        {/* total hours box removed per design */}
      </section>

      <div style={{ marginTop: 12 }}>
        {!minHoursSatisfied && (
          <div className={styles.warningBox} role="alert">Minimum 4 hours required to add availability ({totalHours} hrs)</div>
        )}

        <div className={styles.actionsRight}>
          <button
            className={styles.primaryBtn}
            disabled={!minHoursSatisfied}
            title={!minHoursSatisfied ? 'Total shift must be at least 4 hours' : 'Add Availability'}
            onClick={() => {
              // TODO: call backend API to save availability
              console.log('Add Availability', { selectedDate, selectedFacility, startTime, endTime });
            }}
          >
            Add Availability
          </button>
        </div>
      </div>
    </div>
  );
}
