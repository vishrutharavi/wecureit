"use client";

import React, { useState } from "react";
import styles from "../../patient.module.scss";

// Inline SectionCard and ReadField so Address is self-contained when Card.tsx is removed
export function ReadField({ children }: { children: React.ReactNode }) {
  return <div className={styles.readField}>{children}</div>;
}

type SectionCardProps = {
  icon?: React.ReactNode;
  title: string;
  subtitle?: string;
  children?: React.ReactNode;
};

function SectionCard({ icon, title, subtitle, children }: SectionCardProps) {
  return (
    <div className={`${styles.panelWhite} ${styles.profilePanel} ${styles.sectionCard}`}>
      <div className={styles.sectionHeader}>
        <div className={styles.sectionIcon}>{icon}</div>
        <div>
          <div className={styles.sectionTitle}>{title}</div>
          {subtitle ? <div className={styles.sectionSubtitle}>{subtitle}</div> : null}
        </div>
      </div>

      <div className={styles.cardContent}>{children}</div>
    </div>
  );
}
import { FiMapPin } from "react-icons/fi";

export default function Address({
  street: initialStreet = "",
  city: initialCity = "",
  state: initialState = "",
  zip: initialZip = "",
}: Partial<Record<string, string>> = {}) {
  const [editMode, setEditMode] = useState(false);

  const [street, setStreet] = useState(() => {
    try {
      const raw = typeof window !== "undefined" ? localStorage.getItem("patientProfile") : null;
      if (raw) {
        const p = JSON.parse(raw);
        return (p?.address?.street as string) || initialStreet;
      }
    } catch {}
    return initialStreet;
  });

  const [city, setCity] = useState(() => {
    try {
      const raw = typeof window !== "undefined" ? localStorage.getItem("patientProfile") : null;
      if (raw) {
        const p = JSON.parse(raw);
        return (p?.address?.city as string) || initialCity;
      }
    } catch {}
    return initialCity;
  });

  const [stateVal, setStateVal] = useState(() => {
    try {
      const raw = typeof window !== "undefined" ? localStorage.getItem("patientProfile") : null;
      if (raw) {
        const p = JSON.parse(raw);
        return (p?.address?.state as string) || initialState;
      }
    } catch {}
    return initialState;
  });

  const [zip, setZip] = useState(() => {
    try {
      const raw = typeof window !== "undefined" ? localStorage.getItem("patientProfile") : null;
      if (raw) {
        const p = JSON.parse(raw);
        return (p?.address?.zip as string) || initialZip;
      }
    } catch {}
    return initialZip;
  });

  // Address manages its own local edit state so editing this section does not affect others.

  // local actions (section-level edit controls)
  function startEdit() {
    setEditMode(true);
  }

  function doSave() {
    try {
      const raw = localStorage.getItem("patientProfile");
      const p = raw ? JSON.parse(raw) : {};
      p.address = { street, city, state: stateVal, zip };
      localStorage.setItem("patientProfile", JSON.stringify(p));
    } catch {}
    setEditMode(false);
  }

  function doCancel() {
    try {
      const raw = localStorage.getItem("patientProfile");
      if (raw) {
        const p = JSON.parse(raw);
        if (p?.address) {
          setStreet(p.address.street || "");
          setCity(p.address.city || "");
          setStateVal(p.address.state || "");
          setZip(p.address.zip || "");
        }
      }
    } catch {}
    setEditMode(false);
  }

  return (
    <SectionCard icon={<FiMapPin size={18} />} title="Address" subtitle="Your residential address">
      <div className={styles.sectionGrid}>
        <div className={styles.sectionActionsRight}>
          {!editMode ? (
            <button className={styles.editProfileBtn} onClick={startEdit}>
              ✎ Edit
            </button>
          ) : (
            <div className={styles.actionRow}>
              <button onClick={doCancel} className={styles.cancelSecondary}>
                ✕ Cancel
              </button>
              <button className={styles.viewAppointmentsBtn} onClick={doSave}>
                Save Changes
              </button>
            </div>
          )}
        </div>

        <div>
          <div className={"fieldLabel"}>Street Address</div>
          {editMode ? (
            <input className={styles.inputField} value={street} onChange={(e) => setStreet(e.target.value)} />
          ) : (
            <div className={styles.fullWidth}>
              <ReadField>{street || "No street on file"}</ReadField>
            </div>
          )}
        </div>

        <div className={styles.threeColGrid}>
          <div>
            <div className={"fieldLabel"}>City</div>
            {editMode ? (
              <input className={styles.inputField} value={city} onChange={(e) => setCity(e.target.value)} />
            ) : (
              <ReadField>{city || "-"}</ReadField>
            )}
          </div>

          <div>
            <div className={"fieldLabel"}>State</div>
            {editMode ? (
              <input className={styles.inputField} value={stateVal} onChange={(e) => setStateVal(e.target.value)} />
            ) : (
              <ReadField>{stateVal || "-"}</ReadField>
            )}
          </div>

          <div>
            <div className={"fieldLabel"}>ZIP Code</div>
            {editMode ? (
              <input className={styles.inputField} value={zip} onChange={(e) => setZip(e.target.value)} />
            ) : (
              <ReadField>{zip || "-"}</ReadField>
            )}
          </div>
        </div>
      </div>
    </SectionCard>
  );
}
