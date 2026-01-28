"use client";

import styles from "../doctor.module.scss";

type Props = {
  doctorName?: string;
};

export default function DoctorHeader({
  doctorName = "Dr. Sarah Mitchell",
}: Props) {
  const raw = doctorName || "";
  // remove any existing Dr. prefix (with or without a period) then re-prefix once
  const normalized = raw.replace(/^Dr\.?\s*/i, "");
  const displayName = `Dr. ${normalized}`.trim();

  return (
    <div className={styles.header}>
      <div>
        <h1 className={styles.title}>Welcome, <span className={styles.welcomeDoctorName}>{displayName}</span></h1>
      </div>
    </div>
  );
}
