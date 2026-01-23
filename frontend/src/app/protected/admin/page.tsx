"use client";

import { useState } from "react";
import styles from "./admin.module.scss";
import AdminHeader from "./components/AdminHeader";
import AdminTabs from "./components/AdminTabs";
import DoctorGrid from "./components/Doctors/DoctorGrid";
import FacilityGrid from "./components/Facilities/FacilityGrid";
import AddDoctorModal from "./components/Doctors/AddDoctorModal";
import AddFacilityModal from "./components/Facilities/AddFacilityModal";
import type { Doctor } from "./types";
import { useDoctors } from "./components/Doctors/useDoctors";
import { auth } from "@/lib/firebase";
import { deactivateDoctor } from "@/lib/admin/adminApi";

export default function AdminPage() {
  const [tab, setTab] = useState<"doctors" | "facilities">("doctors");

  const [showAdd, setShowAdd] = useState(false);
  const [editingDoctor, setEditingDoctor] = useState<Doctor | null>(null);

  // fetch doctors and facilities from hooks
  const { doctors: fetchedDoctors, loading: doctorsLoading } = useDoctors();

  // local only for newly created/updated doctors so UI can optimistically show changes
  const [localAddedDoctors, setLocalAddedDoctors] = useState<Doctor[]>([]);
  const [doctorOverrides, setDoctorOverrides] = useState<Record<string, Doctor>>({});

  const doctors = [
    ...localAddedDoctors,
    ...(fetchedDoctors ?? []).map((d) => doctorOverrides[d.id] ?? d),
  ];
  const [deletedDoctorIds, setDeletedDoctorIds] = useState<Set<string>>(new Set());

  const visibleDoctors = doctors.filter((d) => !deletedDoctorIds.has(d.id));

  async function handleDelete(doctor: Doctor) {
    const user = auth.currentUser;
    if (!user) {
      alert("Not authenticated");
      return;
    }

    const ok = confirm(`Delete doctor ${doctor.name ?? doctor.email}?`);
    if (!ok) return;

    try {
      const token = await user.getIdToken();
      await deactivateDoctor(token, doctor.id);
      // optimistically remove from UI
      setDeletedDoctorIds((prev) => new Set(prev).add(doctor.id));
      // also remove from any localAddedDoctors or overrides if present
      setLocalAddedDoctors((prev) => prev.filter((d) => d.id !== doctor.id));
      setDoctorOverrides((prev) => {
        const copy = { ...prev };
        delete copy[doctor.id];
        return copy;
      });
    } catch (err: unknown) {
      console.error("failed to delete doctor", err);
  const msg = (err as { message?: string })?.message ?? "Failed to delete doctor";
      alert(msg);
    }
  }
  

  return (
    <div className={styles.wrapper}>
      <div className={styles.content}>
        <AdminHeader />
        <AdminTabs active={tab} onChange={setTab} />

        <div className={styles.card}>
          <div className={styles.cardHeader}>
            <div>
              <h2>
                {tab === "doctors" ? "Doctor Management" : "Facility Management"}
              </h2>
              <p>
                {tab === "doctors"
                  ? "Create and manage doctors"
                  : "Add and manage medical facilities with rooms"}
              </p>
            </div>

              <button
                className={styles.primaryBtn}
                data-testid="admin-add-btn"
                onClick={() => {
                  setEditingDoctor(null); 
                  setShowAdd(true);
      }}
              >
                {tab === "doctors" ? "Add Doctor" : "Add Facility"}
              </button>
          </div>

          {tab === "doctors" ? (
            !doctorsLoading ? (
              <DoctorGrid
                doctors={visibleDoctors}
                onEdit={(d) => {
                  setEditingDoctor(d);
                  setShowAdd(true);
                }}
                onDelete={handleDelete}
              />
            ) : (
              <div>Loading doctors...</div>
            )
            ) : (
            <FacilityGrid />
          )}
        </div>
      </div>
      {showAdd && (
        tab === "doctors" ? (
          <AddDoctorModal
            initialDoctor={editingDoctor ?? undefined}
            onCreated={(d) => setLocalAddedDoctors((prev: Doctor[]) => [d, ...prev])}
            onUpdated={(d: Doctor) => setDoctorOverrides((prev) => ({ ...prev, [d.id]: d }))}
            onClose={() => setShowAdd(false)}
          />
        ) : (
          <AddFacilityModal onClose={() => setShowAdd(false)} />
        )
      )}
    </div>
  );
}



