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

export default function AdminPage() {
  const [tab, setTab] = useState<"doctors" | "facilities">("doctors");

  const [showAdd, setShowAdd] = useState(false);
  const [editingDoctor, setEditingDoctor] = useState<Doctor | null>(null);

  const [doctors, setDoctors] = useState<Doctor[]>([
    // sample starter data so UI is visible
    {
      id: "1",
      name: "Dr. Sarah Mitchell",
      email: "sarah.mitchell@hospital.com",
      gender: "Female",
      specialties: ["Cardiology", "General Practice"],
      states: ["Washington DC", "Maryland"],
    },
    {
      id: "2",
      name: "Dr. James Rodriguez",
      email: "james.rodriguez@hospital.com",
      gender: "Male",
      specialties: ["Orthopedics", "General Practice"],
      states: ["Virginia"],
    },
  ]);
  

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
            <DoctorGrid
              doctors={doctors}
              onEdit={(d) => {
                setEditingDoctor(d);
                setShowAdd(true);
              }}
            />
          ) : (
            <FacilityGrid />
          )}
        </div>
      </div>
      {showAdd && (
        tab === "doctors" ? (
          <AddDoctorModal
            initialDoctor={editingDoctor ?? undefined}
            onCreated={(d) => setDoctors((prev) => [d, ...prev])}
            onUpdated={(d: Doctor) => setDoctors((prev) => prev.map(p => p.id === d.id ? d : p))}
            onClose={() => setShowAdd(false)}
          />
        ) : (
          <AddFacilityModal onClose={() => setShowAdd(false)} />
        )
      )}
    </div>
  );
}



