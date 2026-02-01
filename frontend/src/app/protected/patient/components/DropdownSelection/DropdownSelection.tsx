"use client";

import React, { useEffect, useState } from "react";
import styles from "../../patient.module.scss";
import { FiActivity } from "react-icons/fi";
import { apiFetch, showInlineToast } from '@/lib/api';

type Doctor = { id: string; name: string; specialties: Array<{ code?: string; name?: string }>; facilities: Array<{ id: string; name?: string }> };
type Facility = { id: string; name: string; specialties?: Array<{ code?: string; name: string }> };
type Specialty = { id?: string; name: string };

type Props = {
  onChange: (selection: { doctor?: Doctor | null; facility?: Facility | null; specialty?: Specialty | null }) => void;
};

type BookingResponse = {
  specialties?: Array<{ code: string; name: string }>,
  facilities?: Array<{ id: string; name: string; specialties?: Array<{ code?: string; name?: string }> }>,
  doctors?: Array<{
    id: string,
    displayName?: string,
    name?: string,
    specialties?: Array<{ code?: string; name?: string }>,
    facilities?: Array<{ id: string; name?: string }>
  }>
}

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
  

  // Intersection of both filters
  const filteredDoctors = React.useMemo(() => {
    return doctors.filter((d) => {
      if (selectedSpecialty) {
        const ok = (d.specialties || []).some(s => s.code === selectedSpecialty || (s.name && s.name === selectedSpecialty));
        if (!ok) return false;
      }
      if (selectedFacility) {
        const ok = (d.facilities || []).some(f => f.id === selectedFacility);
        if (!ok) return false;
      }
      return true;
    });
  }, [doctors, selectedSpecialty, selectedFacility]);

  const facilitySupportsSpecialty = (fac: Facility, specCode: string | "") => {
    if (!specCode) return true;
    return (fac.specialties || []).some(s => (s.code && s.code === specCode) || s.name === specCode);
  };


  const docWorksAtFacility = (doc: Doctor, facId: string | "") => {
    if (!facId) return true;
    return (doc.facilities || []).some(f => f.id === facId);
  };

  const availableFacilities = React.useMemo(() => {
    // If doctor and specialty selected -> intersection of doctor's facilities and facilities that support specialty
    if (selectedDoctor && selectedSpecialty) {
      const doc = doctors.find(d => d.id === selectedDoctor);
      if (!doc) return [];
      return facilities.filter(f => docWorksAtFacility(doc, f.id) && facilitySupportsSpecialty(f, selectedSpecialty));
    }
    // If only doctor selected -> facilities the doctor works at
    if (selectedDoctor) {
      const doc = doctors.find(d => d.id === selectedDoctor);
      if (!doc) return [];
      const set = new Set((doc.facilities || []).map(ff => ff.id));
      return facilities.filter(f => set.has(f.id));
    }
    // If only specialty selected -> facilities that support that specialty
    if (selectedSpecialty) {
      return facilities.filter(f => facilitySupportsSpecialty(f, selectedSpecialty));
    }
    // default -> all facilities
    return facilities;
  }, [selectedDoctor, selectedSpecialty, doctors, facilities]);

  const availableSpecialties = React.useMemo(() => {
    // If doctor and facility selected -> intersection of doctor's specialties and facility's specialties
    const uniqSpecs = (arr: Array<{ id?: string; name?: string }>): Specialty[] => {
      const m = new Map<string, Specialty>();
      for (const x of arr) {
        const key = (x.id ?? x.name) || '';
        if (!key) continue;
        if (!m.has(key)) m.set(key, { id: x.id, name: x.name ?? x.id ?? '' });
      }
      return Array.from(m.values());
    };

    if (selectedDoctor && selectedFacility) {
      const doc = doctors.find(d => d.id === selectedDoctor);
      const fac = facilities.find(f => f.id === selectedFacility);
      if (!doc || !fac) return [];
      const docSpecs = new Set((doc.specialties || []).map(s => s.code ?? s.name));
      return uniqSpecs((fac.specialties || []).filter(s => docSpecs.has(s.code ?? s.name)).map(s => ({ id: s.code, name: s.name })));
    }
    // If only doctor selected -> doctor's specialties
    if (selectedDoctor) {
      const doc = doctors.find(d => d.id === selectedDoctor);
      if (!doc) return [];
      return uniqSpecs((doc.specialties || []).map(s => ({ id: s.code, name: s.name })));
    }
    // If only facility selected -> facility's specialties
    if (selectedFacility) {
      const fac = facilities.find(f => f.id === selectedFacility);
      if (!fac) return [];
      return uniqSpecs((fac.specialties || []).map(s => ({ id: s.code, name: s.name })));
    }
    // default -> global specialties filtered by available doctors
    const set = new Set<string>();
    filteredDoctors.forEach((d) => (d.specialties || []).forEach(s => { const key = s.code ?? s.name ?? ''; if (key) set.add(key); }));
    // ensure uniqueness by code/name while preserving order
    const uniqMap = new Map<string, { id?: string; name: string }>();
    for (const s of specialties) {
      const key = (s.id ?? s.name) || '';
      if (!key) continue;
      if (set.has(key) && !uniqMap.has(key)) uniqMap.set(key, { id: s.id, name: s.name });
    }
    return uniqSpecs(Array.from(uniqMap.values()));
  }, [selectedDoctor, selectedFacility, doctors, facilities, specialties, filteredDoctors]);

  // load dropdown data on mount
  useEffect(() => {
    (async () => {
      try {
        const resp = await apiFetch('/api/patients/booking/dropdown-data') as BookingResponse;
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
          if (selectedDoctor && !docs.some(dd => dd.id === selectedDoctor)) setSelectedDoctor("");
          if (selectedFacility && !facs.some(ff => ff.id === selectedFacility)) setSelectedFacility("");
          if (selectedSpecialty && !specs.some(ss => (ss.id ?? ss.name) === selectedSpecialty)) setSelectedSpecialty("");
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

  const isDoctorStillAvailable = (doctorId: string, specialtyVal: string | "", facilityVal: string | "") => {
    return doctors.some((d) => {
      if (d.id !== doctorId) return false;
      if (specialtyVal) {
        const ok = (d.specialties || []).some(s => s.code === specialtyVal || (s.name && s.name === specialtyVal));
        if (!ok) return false;
      }
      if (facilityVal) {
        const ok = (d.facilities || []).some(f => f.id === facilityVal);
        if (!ok) return false;
      }
      return true;
    });
  };

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
                setSelectedDoctor(val);
                // if the newly selected doctor doesn't work at the currently selected facility, clear facility
                if (val && selectedFacility && !isDoctorStillAvailable(val, selectedSpecialty, selectedFacility)) {
                  // determine which of facility/specialty are incompatible and clear them
                  const doc = doctors.find(d => d.id === val);
                  if (doc) {
                    if (selectedFacility && !docWorksAtFacility(doc, selectedFacility)) setSelectedFacility("");
                    if (selectedSpecialty && !(doc.specialties || []).some(s => (s.code && s.code === selectedSpecialty) || s.name === selectedSpecialty)) setSelectedSpecialty("");
                  }
                }
              }}
              disabled={loading}
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
              onChange={(e) => {
                const val = e.target.value;
                setSelectedFacility(val);
                if (selectedDoctor && !isDoctorStillAvailable(selectedDoctor, selectedSpecialty, val)) {
                  setSelectedDoctor("");
                }
                // if currently selected specialty is not supported by the newly selected facility, clear it
                if (val && selectedSpecialty) {
                  const fac = facilities.find(f => f.id === val);
                  if (fac && !facilitySupportsSpecialty(fac, selectedSpecialty)) setSelectedSpecialty("");
                }
              }}
              disabled={loading}
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
              onChange={(e) => {
                const val = e.target.value;
                setSelectedSpecialty(val);
                if (selectedDoctor && !isDoctorStillAvailable(selectedDoctor, val, selectedFacility)) {
                  setSelectedDoctor("");
                }
                // if currently selected facility does not support the newly selected specialty, clear it
                if (selectedFacility && val) {
                  const fac = facilities.find(f => f.id === selectedFacility);
                  if (fac && !facilitySupportsSpecialty(fac, val)) setSelectedFacility("");
                }
              }}
              disabled={loading}
            >
              <option value="">Choose a specialty</option>
              {availableSpecialties.map((s) => (
                <option key={s.id ?? s.name} value={s.id ?? s.name}>{s.name}</option>
              ))}
            </select>
          </div>
          <div className={styles.profileSubtitle}>{availableSpecialties.length} specialties available</div>
        </div>
      </div>
    </div>
  );
}
