import DoctorCard from "./DoctorCard";
import styles from "../../admin.module.scss";
import type { Doctor, State, Speciality } from "../../types";

type Props = {
  doctors: Doctor[];
  onEdit?: (doctor: Doctor) => void;
  onDelete?: (doctor: Doctor) => void;
  metaStates?: State[];
  metaSpecialities?: Speciality[];
};

export default function DoctorGrid({ doctors, onEdit, onDelete, metaStates, metaSpecialities }: Props) {
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
          metaStates={metaStates}
          metaSpecialities={metaSpecialities}
        />
      ))}
    </div>
  );
}
