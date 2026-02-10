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

  const [street, setStreet] = useState<string>(initialStreet);

  const [city, setCity] = useState<string>(initialCity);

  const [stateVal, setStateVal] = useState<string>(initialState);

  const [zip, setZip] = useState<string>(initialZip);

  // Read local profile after mount to avoid SSR/CSR hydration mismatch.
  React.useEffect(() => {
    try {
      const raw = localStorage.getItem("patientProfile");
      if (!raw) return;
      const p = JSON.parse(raw);
      if (!p) return;
      // backend may store address as a string or object
      if (p.address && typeof p.address === 'string') {
        setStreet(p.address || "");
        // if the API also provided structured city/state/zip fields, use them
        setCity(p.city ?? "");
        setStateVal(p.state ?? "");
        setZip(p.zip ?? "");
      } else if (p.address) {
        setStreet(p.address.street || "");
        setCity(p.address.city || "");
        setStateVal(p.address.state || "");
        setZip(p.address.zip || "");
      }
      // top-level fields (me endpoint) may include city/state/zip — prefer them if present
      if (p.city) setCity(p.city);
      if (p.state) setStateVal(p.state);
      if (p.zip) setZip(p.zip);
    } catch {}
  }, []);

  // Address manages its own local edit state so editing this section does not affect others.

  // Address is read-only in the patient portal; editing is disabled here.

  return (
    <SectionCard icon={<FiMapPin size={18} />} title="Address" subtitle="Your residential address">
      <div className={styles.sectionGrid}>
        {/* Editing address from the portal is disabled by product decision; keep read-only view */}

        <div>
          <div className={"fieldLabel"}>Street Address</div>
          <div className={styles.fullWidth}>
            <ReadField>{street || "No street on file"}</ReadField>
          </div>
        </div>

        <div className={styles.threeColGrid}>
          <div>
            <div className={"fieldLabel"}>City</div>
            <ReadField>{city || "-"}</ReadField>
          </div>

          <div>
            <div className={"fieldLabel"}>State</div>
            <ReadField>{stateVal || "-"}</ReadField>
          </div>

          <div>
            <div className={"fieldLabel"}>ZIP Code</div>
            <ReadField>{zip || "-"}</ReadField>
          </div>
        </div>
      </div>
    </SectionCard>
  );
}
