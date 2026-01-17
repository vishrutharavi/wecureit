import { Pencil, Trash2 } from "lucide-react";
import styles from "../../admin.module.scss";
import type { Doctor } from "../../types";

export default function DoctorCard({
  doctor,
  onEdit,
  onDelete,
}: {
  doctor: Doctor;
  onEdit?: (doctor: Doctor) => void;
  onDelete?: (doctor: Doctor) => void;
}) {
  return (
    <div className={styles.itemCard}>
      <div className={styles.itemCardHeader}>
        <div>
          <h3 className={styles.itemCardTitle}>{doctor?.name ?? "Unnamed"}</h3>
          <p className={styles.itemCardEmail}>{doctor?.email ?? ""}</p>
        </div>

        <div className={styles.itemActions}>
          <Pencil
            size={18}
            style={{ cursor: onEdit ? "pointer" : "default" }}
            onClick={() => onEdit?.(doctor)}
          />
          <Trash2
            size={18}
            className={styles.delete}
            style={{ cursor: onDelete ? "pointer" : "default" }}
            onClick={() => onDelete?.(doctor)}
          />
        </div>
      </div>

      <div className={styles.badges} style={{ marginTop: 12 }}>
        {(doctor?.specialties ?? []).map((s: string) => (
          <span key={s} className={styles.badge}>
            {s}
          </span>
        ))}
      </div>
    </div>
  );
}
