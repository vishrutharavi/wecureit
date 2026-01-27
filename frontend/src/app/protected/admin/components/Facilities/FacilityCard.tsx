import { Facility } from "../../types";
import RoomList from "./RoomList";
import styles from "../../admin.module.scss";

export default function FacilityCard({ facility }: { facility: Facility }) {
  return (
    <div className={styles.itemCard}>
      <h3 className={styles.itemCardTitle}>{facility.name}</h3>
      <p className={styles.itemCardEmail}>{facility.city}, {facility.state}</p>

      <RoomList rooms={facility.rooms} />
    </div>
  );
}
