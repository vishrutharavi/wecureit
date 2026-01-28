"use client";

import React from "react";
import styles from "../../doctor.module.scss";
import { FileText, MapPin, Calendar, Clock, User } from "lucide-react";
import { useRouter } from "next/navigation";

type Props = {
  patientName: string;
  ageGender?: string;
  date: string;
  time: string;
  duration?: string;
  complaint?: string;
  facility?: string;
  status?: string;
};

export default function CompletedAppointmentCard({
  patientName,
  ageGender,
  date,
  time,
  duration,
  complaint,
  facility,
  status,
}: Props) {
  const router = useRouter();

  // navigate to referral page with patient query param
  const handleRefer = () => router.push(`/protected/doctor/notes/refer?patient=${encodeURIComponent(patientName)}`);

  return (
    <div className={styles.appointmentCard}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start" }}>
        <div>
          <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 8 }}>
            <User size={16} />
            <div style={{ fontWeight: 800 }}>{patientName}</div>
          </div>
          {ageGender && <div style={{ color: '#666', marginBottom: 12 }}>{ageGender}</div>}

          <div style={{ display: 'flex', gap: 18, alignItems: 'center', color: '#444', marginBottom: 8 }}>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><Calendar size={14} /> <span>{date}</span></div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}><Clock size={14} /> <span>{time}</span></div>
            {duration && <div className={styles.smallBadge}>{duration}</div>}
          </div>

          {complaint && (
            <div style={{ border: '1px solid #f6d7d7', background: '#fffaf9', padding: 10, borderRadius: 6, marginBottom: 8 }}>
              <strong>Chief Complaint:</strong>
              <div style={{ marginTop: 6 }}>{complaint}</div>
            </div>
          )}

          {facility && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#c33' }}><MapPin size={14} /> <span>{facility}</span></div>
          )}
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'flex-end' }}>
          {status && <div className={styles.smallBadge} style={{ background: '#fff', color: 'var(--doctor-primary)', border: '1px solid rgba(239,68,68,0.12)' }}>{status}</div>}
          <button className={styles.primaryBtn} style={{ padding: '0.55rem 1rem', minWidth: 120 }}>
            <FileText size={14} style={{ marginRight: 8 }} /> Notes
          </button>
          <button className={styles.secondaryBtn} onClick={handleRefer}>Refer Specialty</button>
        </div>
      </div>
    </div>
  );
}
