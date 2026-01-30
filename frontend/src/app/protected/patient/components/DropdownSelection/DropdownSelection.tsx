"use client";

import React from "react";
import styles from "../../patient.module.scss";
import { FiActivity } from "react-icons/fi";

type Doctor = { id: string; name: string; specialtyId: string; facilityId: string };
type Facility = { id: string; name: string };
type Specialty = { id: string; name: string };

type Props = {
  onChange: (selection: { doctor?: Doctor | null; facility?: Facility | null; specialty?: Specialty | null }) => void;
};

// Sample data – replace with API integration as needed
const SPECIALTIES: Specialty[] = [
  { id: "s1", name: "Cardiology" },
  { id: "s2", name: "Orthopedics" },
  { id: "s3", name: "Dermatology" },
  { id: "s4", name: "Pediatrics" },
];

const FACILITIES: Facility[] = [
  { id: "f1", name: "Downtown Medical Center" },
  { id: "f2", name: "Alexandria Main Hospital" },
  { id: "f3", name: "Northside Clinic" },
];

const DOCTORS: Doctor[] = [
  { id: "d1", name: "Dr. Sarah Johnson", specialtyId: "s1", facilityId: "f1" },
  { id: "d2", name: "Dr. Michael Chen", specialtyId: "s2", facilityId: "f2" },
  { id: "d3", name: "Dr. Priya Patel", specialtyId: "s3", facilityId: "f1" },
  { id: "d4", name: "Dr. Omar Ali", specialtyId: "s1", facilityId: "f3" },
  { id: "d5", name: "Dr. Emily Park", specialtyId: "s4", facilityId: "f2" },
];

export default function DropdownSelection({ onChange }: Props) {
  const [selectedSpecialty, setSelectedSpecialty] = React.useState<string | "">("");
  const [selectedFacility, setSelectedFacility] = React.useState<string | "">("");
  const [selectedDoctor, setSelectedDoctor] = React.useState<string | "">("");

  // Intersection of both filters
  const filteredDoctors = React.useMemo(() => {
    return DOCTORS.filter((d) => {
      if (selectedSpecialty && d.specialtyId !== selectedSpecialty) return false;
      if (selectedFacility && d.facilityId !== selectedFacility) return false;
      return true;
    });
  }, [selectedSpecialty, selectedFacility]);

  const availableFacilities = React.useMemo(() => {
    const set = new Set<string>();
    filteredDoctors.forEach((d) => set.add(d.facilityId));
    return FACILITIES.filter((f) => set.has(f.id));
  }, [filteredDoctors]);

  const availableSpecialties = React.useMemo(() => {
    const set = new Set<string>();
    filteredDoctors.forEach((d) => set.add(d.specialtyId));
    return SPECIALTIES.filter((s) => set.has(s.id));
  }, [filteredDoctors]);

  React.useEffect(() => {
    // when filters change, clear doctor selection if it no longer exists
    if (selectedDoctor) {
      const stillAvailable = filteredDoctors.some((d) => d.id === selectedDoctor);
      if (!stillAvailable) setSelectedDoctor("");
    }
    // report up
    const doctor = DOCTORS.find((d) => d.id === selectedDoctor) || null;
    const facility = FACILITIES.find((f) => f.id === selectedFacility) || null;
    const specialty = SPECIALTIES.find((s) => s.id === selectedSpecialty) || null;
    onChange({ doctor, facility, specialty });
  }, [selectedDoctor, selectedFacility, selectedSpecialty, filteredDoctors, onChange]);

  return (
    <div>
      <div className={styles.panelWhite}>
        <div className={styles.sectionHeader}>
          <div className={styles.sectionIcon}><FiActivity size={18} /></div>
          <div>
            <div className={styles.sectionTitle}>Appointment Details</div>
            <div className={styles.sectionSubtitle}>Choose your preferences below</div>
          </div>
        </div>

        <div className={styles.cardContent}>
          <div className={styles.fieldLabel}>Select Doctor</div>
          <div className={styles.searchInputWrap}>
            <select
              className={styles.inputField}
              value={selectedDoctor}
              onChange={(e) => setSelectedDoctor(e.target.value)}
            >
              <option value="">Choose a doctor</option>
              {filteredDoctors.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>
          <div className={styles.profileSubtitle}>{filteredDoctors.length} doctors available</div>

          <div style={{ height: 12 }} />

          <div className={styles.fieldLabel}>Select Facility</div>
          <div className={styles.searchInputWrap}>
            <select
              className={styles.inputField}
              value={selectedFacility}
              onChange={(e) => setSelectedFacility(e.target.value)}
            >
              <option value="">Choose a facility</option>
              {availableFacilities.map((f) => (
                <option key={f.id} value={f.id}>{f.name}</option>
              ))}
            </select>
          </div>
          <div className={styles.profileSubtitle}>{availableFacilities.length} facilities available</div>

          <div style={{ height: 12 }} />

          <div className={styles.fieldLabel}>Select Specialty</div>
          <div className={styles.searchInputWrap}>
            <select
              className={styles.inputField}
              value={selectedSpecialty}
              onChange={(e) => setSelectedSpecialty(e.target.value)}
            >
              <option value="">Choose a specialty</option>
              {availableSpecialties.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>
          <div className={styles.profileSubtitle}>{availableSpecialties.length} specialties available</div>
        </div>
      </div>
    </div>
  );
}
