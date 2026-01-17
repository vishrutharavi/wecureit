// src/app/protected/admin/components/Doctors/AddDoctorModal.tsx
"use client";

import styles from "../../admin.module.scss";
import { X } from "lucide-react";
import { useDoctorMeta } from "./useDoctors";
import { useState } from "react";
import { useEffect } from "react";
import { apiFetch } from "@/lib/api";
import type { Doctor } from "../../types";

type Props = {
  onClose: () => void;
  onCreated?: (d: Doctor) => void;
  onUpdated?: (d: Doctor) => void;
  initialDoctor?: Doctor | undefined;
};

export default function AddDoctorModal({ onClose, onCreated, onUpdated, initialDoctor }: Props) {
  const { states, specialities, loading } = useDoctorMeta();

  useEffect(() => {
    // runtime debug: log when modal mounts so user can confirm it's mounted
    console.log("[AddDoctorModal] mount/update, loading=", loading, "states.length=", states.length);
  }, [loading, states.length]);

  const [form, setForm] = useState({
    name: "",
    email: "",
    gender: "",
    password: "",
    stateCodes: [] as string[],
    specialityCodes: [] as string[],
  });
  const [selectedState, setSelectedState] = useState<string>("");

  // if editing an existing doctor, populate the form when modal mounts
  useEffect(() => {
    if (initialDoctor) {
      setForm({
        name: initialDoctor.name ?? "",
        email: initialDoctor.email ?? "",
        gender: initialDoctor.gender ?? "",
        password: "",
        stateCodes: initialDoctor.states ?? [],
        specialityCodes: initialDoctor.specialties ?? [],
      });
    }
  }, [initialDoctor]);

  function toggle(list: string[], value: string) {
    return list.includes(value)
      ? list.filter(v => v !== value)
      : [...list, value];
  }

  async function handleSubmit() {
    if (!form.name || !form.email || !form.gender) {
      alert("Fill all required fields");
      return;
    }

    try {
      if (initialDoctor) {
        // update existing doctor
        const updated = await apiFetch(`/api/admin/doctors/${initialDoctor.id}`, undefined, {
          method: "PUT",
          body: JSON.stringify(form),
        });
        if (onUpdated) onUpdated(updated as Doctor);
      } else {
        const created = await apiFetch("/api/admin/doctors", undefined, {
          method: "POST",
          body: JSON.stringify(form),
        });
        if (onCreated) onCreated(created as Doctor);
      }
    } finally {
      onClose();
    }
  }

  if (loading) return null;

  return (
    <div className={styles['modal-overlay']}>
      <div className={styles['modal-container']}>
        <div className={styles['modal-header']}>
          <h2>{initialDoctor ? "Edit Doctor" : "Add Doctor"}</h2>
          <button onClick={onClose} aria-label="Close modal"><X /></button>
        </div>

        {/* BASIC INFO */}
        <div className={styles['modal-section']}>
          <div className={styles['modal-row-grid']}>
            <input
              className={styles['modal-input']}
              placeholder="Doctor Name"
              value={form.name}
              onChange={e => setForm({ ...form, name: e.target.value })}
            />
            <input
              className={styles['modal-input']}
              placeholder="Email"
              value={form.email}
              onChange={e => setForm({ ...form, email: e.target.value })}
            />

            <input
              type="password"
              className={styles['modal-input']}
              placeholder="Create secure password for doctor"
              value={form.password}
              onChange={e => setForm({ ...form, password: e.target.value })}
            />

            <select
              className={styles['modal-input']}
              value={form.gender}
              onChange={e => setForm({ ...form, gender: e.target.value })}
            >
              <option value="">Select gender</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
        </div>

        {/* STATES: dropdown + added state chips + nested specialties container */}
        <div className={styles['modal-section']}>
          <div className={styles['state-container']}>
            <h4>State</h4>
            <div className={styles['state-row']}>
              <select
                className={styles['state-select']}
                value={selectedState}
                onChange={(e) => {
                  const val = e.target.value;
                  setSelectedState(val);
                  // immediately add the selected state to the form and clear the select
                  if (val && !form.stateCodes.includes(val)) {
                    setForm({ ...form, stateCodes: [...form.stateCodes, val] });
                    setSelectedState("");
                  }
                }}
              >
                <option value="">Select state</option>
                {states.map(s => (
                  <option key={s.code} value={s.code}>{s.name}</option>
                ))}
              </select>
            </div>

            <div style={{ marginBottom: '0.75rem' }}>
              {form.stateCodes.map(code => {
                const s = states.find(st => st.code === code);
                if (!s) return null;
                return (
                  <span key={code} className={styles['state-chip']} style={{ marginRight: 8 }}>
                    {s.name}
                  </span>
                );
              })}
            </div>

            {/* nested specialties container inside the state block */}
            <div>
              <h4>Specialities</h4>
              <div className={styles['specialty-box']}>
                <div className={styles['doctor-specialtyGrid']}>
                  {specialities.map(sp => (
                    <label key={sp.code} className={styles['modal-checkbox']}>
                      <input
                        type="checkbox"
                        checked={form.specialityCodes.includes(sp.code)}
                        onChange={() =>
                          setForm({
                            ...form,
                            specialityCodes: toggle(form.specialityCodes, sp.code),
                          })
                        }
                      />
                      {sp.name}
                    </label>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className={styles['modal-footer']}>
          <button className={styles['secondaryBtn']} onClick={onClose}>
            Cancel
          </button>
          <button className={styles['primaryBtn']} onClick={handleSubmit}>
            Create Doctor
          </button>
        </div>
      </div>
    </div>
  );
}
