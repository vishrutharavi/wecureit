"use client";

import styles from "../../doctor.module.scss";
import { useState } from "react";

export default function AvailabilityView() {
  const [selectedDate, setSelectedDate] = useState<string>("2026-01-22");
  const [selectedFacility, setSelectedFacility] = useState<string | null>(null);
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("17:00");

  const facilities = [
    { id: "1", name: "Downtown Medical Center", city: "Washington DC", rooms: 12 },
    { id: "2", name: "Alexandria Main Hospital", city: "Alexandria", rooms: 15 },
    { id: "3", name: "Bethesda Health Center", city: "Bethesda", rooms: 9 },
  ];

  return (
    <div className={styles.card}>
      <h2>Set Your Availability</h2>
      <p className={styles.subtitle}>
        Choose date, facility, and working hours. Minimum 4 hours per day.
      </p>

      {/* STEP 1: Date */}
      <section className={styles.section}>
        <h4>1. Select Date</h4>
        <div className={styles.dateGrid}>
          {["21", "22", "23", "24", "25", "26", "27"].map((day) => (
            <button
              key={day}
              className={`${styles.dateCard} ${
                selectedDate.endsWith(day) ? styles.selected : ""
              }`}
              onClick={() => setSelectedDate(`2026-01-${day}`)}
            >
              <strong>{day}</strong>
              <span>Jan</span>
            </button>
          ))}
        </div>
      </section>

      {/* STEP 2: Facility */}
      <section className={styles.section}>
        <h4>2. Select Facility</h4>
        <div className={styles.facilityGrid}>
          {facilities.map((f) => (
            <div
              key={f.id}
              className={`${styles.facilityCard} ${
                selectedFacility === f.id ? styles.selected : ""
              }`}
              onClick={() => setSelectedFacility(f.id)}
            >
              <h5>{f.name}</h5>
              <p>{f.city}</p>
              <span className={styles.badge}>{f.rooms} exam rooms</span>
            </div>
          ))}
        </div>
      </section>

      {/* STEP 3: Time */}
      <section className={styles.section}>
        <h4>3. Select Working Hours</h4>
        <div className={styles.timeRow}>
          <div>
            <label>Start Time</label>
            <select value={startTime} onChange={(e) => setStartTime(e.target.value)}>
              <option>09:00</option>
              <option>10:00</option>
              <option>11:00</option>
            </select>
          </div>

          <div>
            <label>End Time</label>
            <select value={endTime} onChange={(e) => setEndTime(e.target.value)}>
              <option>17:00</option>
              <option>18:00</option>
              <option>19:00</option>
            </select>
          </div>
        </div>

        <div className={styles.infoBox}>
          Total hours: <strong>8.0 hours</strong>
        </div>
      </section>

      <div className={styles.actionsRight}>
        <button className={styles.primaryBtn}>Update Availability</button>
      </div>
    </div>
  );
}
