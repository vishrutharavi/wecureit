"use client";

import React from "react";
import styles from "../../doctor.module.scss";
import { MapPin, Calendar, Clock, User, Plus } from "lucide-react";
import { useRouter } from "next/navigation";
import AddNoteModal from "./AddNoteModal";
import ViewNoteModal from "./ViewNoteModal";

type Props = {
  patientName: string;
  ageGender?: string;
  date: string;
  time: string;
  duration?: string;
  complaint?: string;
  facility?: string;
  status?: string;
  appointmentDbId?: string;
  patientId?: string;
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
  appointmentDbId,
  patientId,
}: Props) {
  const router = useRouter();
  const [showAddNote, setShowAddNote] = React.useState(false);
  const [showViewNotes, setShowViewNotes] = React.useState(false);
  const [doctorNameState, setDoctorNameState] = React.useState<string>('');

  React.useEffect(() => {
    try {
      const raw = localStorage.getItem('doctorProfile');
      if (raw) {
        const obj = JSON.parse(raw);
        setDoctorNameState(obj.name ?? obj.email ?? '');
      }
    } catch {}
  }, []);

  // navigate to referral page with patient query param
  const handleRefer = () => router.push(`/protected/doctor/notes/refer?patient=${encodeURIComponent(patientName)}`);

  return (
    <div className={`${styles.facilityCard} ${styles.appointmentCard}`}>
      <div className={styles.appointmentRow} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <div className={`${styles.compactRow} ${styles.mt8}`}>
            <User size={16} />
            <div className={styles.cardTitle}>{patientName}</div>
          </div>
          {ageGender && <div className={`${styles.referralMeta} ${styles.mt12}`}>{ageGender}</div>}

          <div className={styles.dateTimeRow}>
            <div className={styles.compactRow}><Calendar size={14} /> <span>{date}</span></div>
            <div className={styles.compactRow}><Clock size={14} /> <span>{time}</span></div>
            {duration && <div className={styles.smallBadge}>{duration}</div>}
          </div>

          {complaint && (
            <div className={styles.complaintBox}>
              <strong>Chief Complaint:</strong>
              <div className={styles.mt6}>{complaint}</div>
            </div>
          )}

          {facility && (
            <div className={styles.facilityLine}><MapPin size={14} /> <span>{facility}</span></div>
          )}
        </div>

  <div className={styles.appointmentMeta} style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 12 }}>
          {status && <div className={`${styles.smallBadge} ${styles.statusBadge}`}>{status}</div>}
          <>
            <button className={`${styles.viewAppointmentsBtn} ${styles.btnWide}`} onClick={() => setShowAddNote(true)}>
              <span className={styles.iconSpacing}><Plus size={14} /></span> Add Note
            </button>
            <AddNoteModal open={showAddNote} onClose={() => setShowAddNote(false)} patientName={patientName} dateLabel={date} timeLabel={time} appointmentDbId={appointmentDbId} patientId={patientId} />
          </>
          <button className={styles.secondaryBtn} onClick={handleRefer}>Refer Specialty</button>
          <button className={`${styles.secondaryBtn} ${styles.mt6}`} onClick={() => setShowViewNotes(true)}>View Notes</button>
          <ViewNoteModal open={showViewNotes} onClose={() => setShowViewNotes(false)} patientName={patientName} doctorName={doctorNameState} patientAge={ageGender} appointmentDbId={appointmentDbId} patientId={patientId} />
        </div>
      </div>
    </div>
  );
}
