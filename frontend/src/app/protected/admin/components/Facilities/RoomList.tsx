import { Room } from "../../types";
import styles from "../../admin.module.scss";

export default function RoomList({ rooms }: { rooms?: Room[] }) {
  const list = rooms ?? [];
  if (!list.length) return <p className={styles.noRooms}>No rooms</p>;

  return (
    <div className={styles.roomDetails}>
      {list.map((r, idx) => (
        <div key={r.id} className={styles.roomRow}>
          <div className={styles.roomLabel}><strong>Room {idx + 1}:</strong></div>
          <div className={styles.roomText}>
            <span className={styles.roomSpecialtyChip}>General Practice</span>
            {r.specialty && r.specialty.toLowerCase() !== 'general practice' && (
              <span className={styles.roomSpecialtyChip}>{r.specialty}</span>
            )}
          </div>
        </div>
      ))}
    </div>
  );
}