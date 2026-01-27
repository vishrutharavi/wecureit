"use client";

import styles from "../../admin.module.scss";
import { useState } from "react";
import { useDoctorMeta } from "../Doctors/useDoctors";

export default function AddFacilityModal({ onClose }: { onClose: () => void }) {
  const { states } = useDoctorMeta();
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [stateCode, setStateCode] = useState("");

  return (
    <div className={styles['modal-overlay']}>
      <div className={styles['modal-container']}>
        <div className={styles['modal-header']}>
          <h2 className={styles['modal-title']}>Add New Facility</h2>
          <button onClick={onClose}>✕</button>
        </div>

        <div className={styles['facility-info']}>
          All rooms support <b>General Practice</b> by default.
        </div>

        <div className={styles['modal-section']}>
          <h4>Facility Info</h4>

          {/* 1st row: facility name and address side-by-side */}
          <div className={styles['modal-row-grid']} style={{ marginBottom: 12 }}>
            <input
              className={styles['modal-input']}
              placeholder="Facility Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />

            <input
              className={styles['modal-input']}
              placeholder="Address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
            />
          </div>

          {/* 2nd row: state dropdown full width */}
          <div style={{ marginBottom: 12 }}>
            <select
              className={styles['state-select']}
              value={stateCode}
              onChange={(e) => setStateCode(e.target.value)}
              style={{ width: '100%' }}
              required
            >
              <option value="">Select state</option>
              {states.map((s) => (
                <option key={s.code} value={s.code}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className={styles['facility-room']}>
          <h4>Room 1</h4>
          <div className={styles['facility-specialtyGrid']}>
            {["Cardiology", "Pediatrics", "Dermatology"].map((s) => (
              <label key={s} className={styles['facility-checkbox']}>
                <input type="checkbox" /> {s}
              </label>
            ))}
          </div>
        </div>

        <button className={styles.primaryBtn}>Create Facility</button>
      </div>
    </div>
  );
}
