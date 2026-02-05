"use client";

import React from "react";
import styles from "../../patient.module.scss";
import { FiMapPin } from "react-icons/fi";
import { useRouter } from "next/navigation";
import type { Doctor, Facility, Specialty } from '@/app/protected/patient/types';

type Props = {
  doctor?: Doctor | null;
  facility?: Facility | null;
  specialty?: Specialty | null;
};

export default function SelectionSummary({ doctor, facility, specialty }: Props) {
  const router = useRouter();

  const handleContinue = React.useCallback(() => {
    try {
      sessionStorage.setItem("bookingSelection", JSON.stringify({ doctor, facility, specialty }));
    } catch {
      // ignore
    }
    router.push('/protected/patient?tab=datetimeselection');
  }, [doctor, facility, specialty, router]);

  return (
    <div className={styles.summaryWrapper}>
      <div className={styles.sectionHeader}>
        <div className={styles.sectionIcon}><FiMapPin size={18} /></div>
        <div>
          <div className={styles.sectionTitle}>Selection Summary</div>
          <div className={styles.sectionSubtitle}>Review your choices</div>
        </div>
      </div>

      <div className={styles.cardContent}>
        <div className={styles.summaryCard}>
          <div className={styles.labelMuted}>Doctor</div>
          <div className={styles.contentPrimary}>{doctor ? doctor.name : "No doctor selected"}</div>
        </div>

        <div className={styles.summaryCard}>
          <div className={styles.labelMuted}>Facility</div>
          <div className={styles.contentPrimary}>{facility ? facility.name : "No facility selected"}</div>
        </div>

        <div className={styles.summaryCard}>
          <div className={styles.labelMuted}>Specialty</div>
          <div className={styles.contentPrimary}>{specialty ? specialty.name : "No specialty selected"}</div>
        </div>

        {doctor && facility && specialty && (
          <button className={styles.continueBtn} onClick={handleContinue}>
            Continue to Date & Time Selection
          </button>
        )}
      </div>
    </div>
  );
}
