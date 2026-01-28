"use client";

import React from "react";
import styles from "../../doctor.module.scss";

type Facility = {
  id: string;
  name: string;
  city?: string;
  rooms?: number;
  address?: string;
};

type Props = {
  facilities: Facility[];
  selectedFacility: string | null;
  setSelectedFacility: (id: string | null) => void;
};

export default function FacilitySelector({ facilities, selectedFacility, setSelectedFacility }: Props) {
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
        <div style={{ color: '#666' }}>Choose a facility</div>
        <div>
          <button
            type="button"
            className={styles.secondaryBtn}
            onClick={() => setSelectedFacility(null)}
            disabled={selectedFacility === null}
            aria-label="Clear facility selection"
          >
            Clear selection
          </button>
        </div>
      </div>
      <div className={styles.facilityGrid}>
        {facilities.map((f) => (
          <button
            key={f.id}
            aria-pressed={selectedFacility === f.id}
            className={`${styles.facilityCard} ${selectedFacility === f.id ? styles.selected : ''}`}
            onClick={() => setSelectedFacility(f.id)}
          >
            <div style={{ fontWeight: 800, fontSize: 16, marginBottom: 6 }}>{f.name}</div>
            { (f.city || f.address) && <div style={{ color: '#666', fontSize: 13, marginBottom: 8 }}>{f.city || f.address}</div> }
            {typeof f.rooms !== 'undefined' && (
              <span className={styles.badge}>{f.rooms} exam rooms</span>
            )}
          </button>
        ))}
      </div>
    </div>
  );
}
