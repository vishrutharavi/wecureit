import { Facility } from "../../types";
import RoomList from "./RoomList";
import styles from "../../admin.module.scss";
import { MapPin, Edit2, Trash2 } from "lucide-react";
import { useState } from "react";
import AddFacilityModal from "./AddFacilityModal";
import { auth } from "@/lib/firebase";
import { deactivateFacility } from "@/lib/admin/adminApi";
export default function FacilityCard({ facility, onUpdated }: { facility: Facility; onUpdated?: (updated?: Facility) => void }) {
  const [showEdit, setShowEdit] = useState(false);
  const rooms = facility.rooms ?? [];
  const totalRooms = rooms.length;

  const specialtySet = new Set<string>();
  // Always include General Practice
  specialtySet.add("General Practice");
  rooms.forEach((r) => {
    if (r.specialty) specialtySet.add(r.specialty);
  });
  const specialties = Array.from(specialtySet);

  async function handleDelete() {
    const ok = confirm(`Delete facility ${facility.name ?? ''}?`);
    if (!ok) return;

    const user = auth.currentUser;
    if (!user) {
      alert('Not authenticated');
      return;
    }

    try {
      const token = await user.getIdToken();
      await deactivateFacility(token, String(facility.id));
      // signal facilities list to reload
      try { window.dispatchEvent(new CustomEvent('wecureit:facilities-changed')); } catch {}
    } catch (err) {
      console.error('Failed to delete facility', err);
      const msg = err instanceof Error ? err.message : String(err);
      alert(msg || 'Failed to delete facility');
    }
  }

  return (
    <div className={styles.itemCard}>
      <div className={styles.facilityHeader}>
        <h3 className={styles.itemCardTitle}>{facility.name}</h3>
        <div className={styles.facilityActions}>
          <button className={styles.iconBtn} title="Edit" onClick={() => setShowEdit(true)}>
            <Edit2 />
          </button>
          <button className={styles.iconBtn} title="Delete" onClick={handleDelete}>
            <Trash2 className={styles.delete} />
          </button>
        </div>
      </div>

      <div className={styles.locationRow}>
  <MapPin className={styles.locationIcon} />
        <p className={styles.itemCardEmail}>{facility.city}, {facility.state}</p>
      </div>

      {facility.address && (
        <div className={styles.facilityAddress}>
          <div className={styles.addressLabel}>Address:</div>
          <div className={styles.addressText}>{facility.address}</div>
        </div>
      )}

      <div className={styles.facilityMetaRow}>
        <div className={styles.totalRoomsLabel}>Total Rooms:</div>
        <span className={styles.roomsCountBadge}>{totalRooms} rooms</span>
      </div>

      <div className={styles.supportedSpecialties}>
        <div className={styles.supportedLabel}>All Supported Specialities:</div>
        <div className={styles.specialtyChips}>
          {specialties.map((s) => (
            <span key={s} className={styles.facilitySpecialtyChip}>{s}</span>
          ))}
        </div>
      </div>

      <hr className={styles.cardDivider} />

      <div className={styles.roomDetailsTitle}>Room Details:</div>

      <RoomList rooms={rooms} />

      {showEdit && (
        <AddFacilityModal
          onClose={() => setShowEdit(false)}
          initialFacility={facility}
          onSaved={(updated) => {
            setShowEdit(false);
            if (onUpdated) onUpdated(updated);
          }}
        />
      )}
    </div>
  );
}
