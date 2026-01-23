import DoctorCard from "./DoctorCard";
import styles from "../../admin.module.scss";
import type { Doctor } from "../../types";

type Props = {
  doctors: Doctor[];
  onEdit?: (doctor: Doctor) => void;
  onDelete?: (doctor: Doctor) => void;
};

export default function DoctorGrid({ doctors, onEdit, onDelete }: Props) {
  if (!doctors.length) {
    return <p style={{ padding: "1.5rem" }}>No doctors added yet.</p>;
  }

  return (
    <div className={styles['doctorGrid']}>
      {doctors.map((doctor) => (
        <DoctorCard
          key={doctor.id}
          doctor={doctor}
          onEdit={onEdit ? () => onEdit(doctor) : undefined}
          onDelete={onDelete ? () => onDelete(doctor) : undefined}
        />
      ))}
    </div>
  );
}
