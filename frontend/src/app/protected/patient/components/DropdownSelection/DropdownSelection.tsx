"use client";

import React, { useEffect, useState } from "react";
import styles from "../../patient.module.scss";
import { FiActivity } from "react-icons/fi";
import { apiFetch, showInlineToast } from '@/lib/api';
import type { Doctor, Facility, Specialty, BookingResponse } from '@/app/protected/patient/types';

type Props = {
  onChange: (selection: { doctor?: Doctor | null; facility?: Facility | null; specialty?: Specialty | null }) => void;
};

// BookingResponse type is imported from shared types

// runtime data loaded from server
const EMPTY_SPECIALTIES: Specialty[] = [];
const EMPTY_FACILITIES: Facility[] = [];
const EMPTY_DOCTORS: Doctor[] = [];

export default function DropdownSelection({ onChange }: Props) {
  const [selectedSpecialty, setSelectedSpecialty] = useState<string | "">("");
  const [selectedFacility, setSelectedFacility] = useState<string | "">("");
  const [selectedDoctor, setSelectedDoctor] = useState<string | "">("");

  const [specialties, setSpecialties] = useState<Specialty[]>(EMPTY_SPECIALTIES);
  const [facilities, setFacilities] = useState<Facility[]>(EMPTY_FACILITIES);
  const [doctors, setDoctors] = useState<Doctor[]>(EMPTY_DOCTORS);
  const [loading, setLoading] = useState<boolean>(false);
  
  // load dropdown data on mount
  useEffect(() => {
    (async () => {
      try {
        // include workDate from any existing bookingSelection in sessionStorage so server can apply facility locks
        let url = '/api/patients/booking/dropdown-data';
        try {
          const raw = sessionStorage.getItem('bookingSelection');
          if (raw) {
            const parsed = JSON.parse(raw || '{}');
            if (parsed && parsed.date) {
              const params = new URLSearchParams();
              params.set('workDate', parsed.date);
              url = url + `?${params.toString()}`;
            }
          }
        } catch {
          // ignore session read errors
        }
        const resp = await apiFetch(url) as BookingResponse;
        if (resp) {
          // map specialties
          const specs = Array.isArray(resp.specialties) ? resp.specialties.map((s) => ({ id: s.code, name: s.name })) : [];
          setSpecialties(specs);
    // map facilities (include specialties offered at facility)
          const facs = Array.isArray(resp.facilities) ? resp.facilities.map((f) => ({ id: f.id, name: f.name, specialties: Array.isArray(f.specialties) ? f.specialties.map((s: { code?: string; name?: string }) => ({ code: s.code, name: s.name ?? s.code ?? '' })) : [] })) : [];
          setFacilities(facs);
          // map doctors
          const docs = Array.isArray(resp.doctors) ? resp.doctors.map((d) => ({ id: d.id, name: d.displayName ?? d.name ?? 'Doctor', specialties: (d.specialties || []).map((s) => ({ code: s.code, name: s.name })), facilities: (d.facilities || []).map((fd) => ({ id: fd.id, name: fd.name })) })) : [];
          setDoctors(docs);
        }
      } catch (err) {
        console.error('Failed to load dropdown data', err);
        showInlineToast('Failed to load doctors and facilities');
      }
    })();
  }, []);

  // call backend to compute filtered options whenever selection changes. Debounced to avoid rapid calls.
  useEffect(() => {
    let mounted = true;
    const timer = setTimeout(async () => {
      try {
        setLoading(true);
        const params = new URLSearchParams();
        if (selectedFacility) params.set('facilityId', selectedFacility);
        if (selectedSpecialty) params.set('specialityCode', selectedSpecialty);
        if (selectedDoctor) params.set('doctorId', selectedDoctor);
        // include workDate from any existing bookingSelection in sessionStorage so server can apply facility locks
        try {
          const raw = sessionStorage.getItem('bookingSelection');
          if (raw) {
            const parsed = JSON.parse(raw || '{}');
            if (parsed && parsed.date) params.set('workDate', parsed.date);
          }
        } catch {
          // ignore
        }
        const url = '/api/patients/booking/dropdown-data' + (params.toString() ? `?${params.toString()}` : '');
        const resp = await apiFetch(url) as BookingResponse;
        if (!mounted) return;
        if (resp) {
          const specs = Array.isArray(resp.specialties) ? resp.specialties.map((s) => ({ id: s.code, name: s.name })) : [];
          setSpecialties(specs);
          const facs = Array.isArray(resp.facilities) ? resp.facilities.map((f) => ({ id: f.id, name: f.name, specialties: Array.isArray(f.specialties) ? f.specialties.map((s: { code?: string; name?: string }) => ({ code: s.code, name: s.name ?? s.code ?? '' })) : [] })) : [];
          setFacilities(facs);
          const docs = Array.isArray(resp.doctors) ? resp.doctors.map((d) => ({ id: d.id, name: d.displayName ?? d.name ?? 'Doctor', specialties: (d.specialties || []).map((s) => ({ code: s.code, name: s.name })), facilities: (d.facilities || []).map((fd) => ({ id: fd.id, name: fd.name })) })) : [];
          setDoctors(docs);

          // if current selections are no longer present in server response, clear them
          // Preserve selections when the server returned NO facilities (user wants selections kept and counts shown as 0)
          // Only clear a selection when the server returned a non-empty list for that type and the selected value is not included.
          // Additionally, we avoid clearing doctor/specialty when there are zero facilities in the response.
          if (selectedDoctor && docs.length > 0 && facs.length > 0 && !docs.some(dd => dd.id === selectedDoctor)) setSelectedDoctor("");
          if (selectedFacility && facs.length > 0 && !facs.some(ff => ff.id === selectedFacility)) setSelectedFacility("");
          if (selectedSpecialty && specs.length > 0 && facs.length > 0 && !specs.some(ss => (ss.id ?? ss.name) === selectedSpecialty)) setSelectedSpecialty("");
        }
      } catch (err) {
        console.error('Failed to load filtered dropdown data', err);
        showInlineToast('Failed to update doctors and facilities');
      } finally {
        if (mounted) setLoading(false);
      }
    }, 250);
    return () => {
      mounted = false;
      clearTimeout(timer);
    };
  }, [selectedDoctor, selectedFacility, selectedSpecialty]);

  // Client now relies on server-side validation of compatibility; no local availability checks.

  function handleClear() {
    setSelectedDoctor("");
    setSelectedFacility("");
    setSelectedSpecialty("");
    onChange({ doctor: null, facility: null, specialty: null });
  }

  useEffect(() => {
    // report up whenever selection changes
    const doctor = doctors.find((d) => d.id === selectedDoctor) || null;
    const facility = facilities.find((f) => f.id === selectedFacility) || null;
    const specialty = specialties.find((s) => (s.id ?? s.name) === selectedSpecialty) || null;
    onChange({ doctor, facility, specialty });
  }, [selectedDoctor, selectedFacility, selectedSpecialty, onChange, doctors, facilities, specialties]);

  return (
    <div>
      <div className={styles.panelWhite}>
        <div className={styles.sectionHeader}>
          <div className={styles.sectionIcon}><FiActivity size={18} /></div>
          <div>
            <div className={styles.sectionTitle}>Appointment Details</div>
            <div className={styles.sectionSubtitle}>Choose your preferences below</div>
          </div>
          <div style={{ marginLeft: 'auto' }}>
            <button
              type="button"
              onClick={handleClear}
              aria-label="Clear selections"
              className={styles.clearButton}
            >
              Clear
            </button>
          </div>
        </div>

        <div className={styles.cardContent}>
          <div className={styles.fieldLabel}>Select Doctor</div>
          <div className={styles.searchInputWrap}>
            <select
              className={styles.inputField}
              value={selectedDoctor}
              onChange={(e) => {
                const val = e.target.value;
                // simply update selection; server will return a filtered set and client effect will clear invalid selections
                setSelectedDoctor(val);
              }}
              disabled={loading}
            >
              <option value="">Choose a doctor</option>
              {doctors.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          </div>
          <div className={styles.profileSubtitle}>{doctors.length} doctors available</div>

          <div style={{ height: 12 }} />

          <div className={styles.fieldLabel}>Select Facility</div>
          <div className={styles.searchInputWrap}>
            <select
              className={styles.inputField}
              value={selectedFacility}
              onChange={(e) => {
                const val = e.target.value;
                // update facility selection; server will provide compatibility updates
                setSelectedFacility(val);
              }}
              disabled={loading}
            >
              <option value="">Choose a facility</option>
              {facilities.map((f) => (
                <option key={f.id} value={f.id}>{f.name}</option>
              ))}
            </select>
          </div>
          <div className={styles.profileSubtitle}>{facilities.length} facilities available</div>

          <div style={{ height: 12 }} />

          <div className={styles.fieldLabel}>Select Specialty</div>
          <div className={styles.searchInputWrap}>
            <select
              className={styles.inputField}
              value={selectedSpecialty}
              onChange={(e) => {
                const val = e.target.value;
                // update specialty selection; server will respond with filtered lists
                setSelectedSpecialty(val);
              }}
              disabled={loading}
            >
              <option value="">Choose a specialty</option>
              {specialties.map((s) => (
                <option key={s.id ?? s.name} value={s.id ?? s.name}>{s.name}</option>
              ))}
            </select>
          </div>
          <div className={styles.profileSubtitle}>{specialties.length} specialties available</div>
        </div>
      </div>
    </div>
  );
}
