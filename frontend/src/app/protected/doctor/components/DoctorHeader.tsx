"use client";

import styles from "../doctor.module.scss";

type Props = {
  doctorName?: string;
  primarySpecialty?: string;
};

export default function DoctorHeader({
  doctorName = "Dr. Sarah Mitchell",
  primarySpecialty = "Cardiology",
}: Props) {
  return (
    <div className={styles.header}>
      <div>
        <h1 className={styles.title}>Welcome, {doctorName}</h1>
        <p className={styles.subtitle}>{primarySpecialty}</p>
      </div>
    </div>
  );
}
