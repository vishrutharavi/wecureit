"use client";

import styles from "../../admin.module.scss";
import { useState, useEffect } from "react";
import { auth } from "@/lib/firebase";
import { createDoctor, addDoctorLicense, getDoctors, updateDoctor, deactivateDoctorLicense } from "@/lib/admin/adminApi";
import { useDoctorMeta } from "./useDoctors";
import type { Doctor } from "../../types";
import { Trash2 } from "lucide-react";

type Props = {
  onClose: () => void;
  onCreated?: (d: Doctor) => void;
  onUpdated?: (d: Doctor) => void;
  initialDoctor?: Doctor;
};

export default function AddDoctorModal({
  onClose,
  onCreated,
  onUpdated,
  initialDoctor,
}: Props) {
  const [form, setForm] = useState({
    name: "",
    email: "",
    gender: "",
    password: "",
  });

  const [isSubmitting, setIsSubmitting] = useState(false);

  const { states, specialities } = useDoctorMeta();

  const [licenses, setLicenses] = useState<Array<{ licenseId?: string; stateCode: string; specialityCodes: string[] }>>([
    { stateCode: "", specialityCodes: [] },
  ]);
  // map of stateCode -> array of licenseIds that already exist on the server
  const [originalLicenseMap, setOriginalLicenseMap] = useState<Record<string, string[]>>({});
  // set of existing pairs 'STATE|SPEC' to help detect new additions
  const [originalPairs, setOriginalPairs] = useState<Set<string>>(new Set());

  function addStateLicense() {
    setLicenses((s) => [...s, { stateCode: "", specialityCodes: [] }]);
  }

  function toggleSpeciality(licenseIdx: number, code: string) {
    setLicenses((prev) => {
      const copy = prev.map((p) => ({ ...p, specialityCodes: [...p.specialityCodes] }));
      const arr = copy[licenseIdx].specialityCodes;
      const idx = arr.indexOf(code);
      if (idx >= 0) arr.splice(idx, 1);
      else arr.push(code);
      return copy;
    });
  }

  useEffect(() => {
    if (initialDoctor) {
  // This effect updates the controlled form when `initialDoctor` changes.
  // We intentionally call setState here to populate the edit form. The
  // react-hooks/set-state-in-effect rule warns about cascading renders
  // but in this component the update is deliberate and scoped to the
  // `initialDoctor` prop change only.
      setForm({
        name: initialDoctor.name ?? "",
        email: initialDoctor.email ?? "",
        gender: initialDoctor.gender ?? "",
        password: "",
      });

      // fetch existing licenses for this doctor and pre-populate the grid
      (async () => {
        try {
          const token = await auth.currentUser?.getIdToken();
          if (!token) return;
          const licList = await (await import('@/lib/admin/adminApi')).getDoctorLicenses(token, initialDoctor.id);

          // group by stateCode and remember license ids per speciality
          const map = new Map<string, string[]>();
          const idMap: Record<string, string[]> = {};
          const pairs = new Set<string>();
          for (const l of licList || []) {
            const sc = (l.stateCode || '').toUpperCase();
            const sp = (l.specialityCode || '').toUpperCase();
            const lid = l.id;
            if (!map.has(sc)) map.set(sc, []);
            if (!map.get(sc)!.includes(sp)) map.get(sc)!.push(sp);

            if (!idMap[sc]) idMap[sc] = [];
            if (lid && !idMap[sc].includes(lid)) idMap[sc].push(lid);

            pairs.add(`${sc}|${sp}`);
          }

          const grouped = Array.from(map.entries()).map(([stateCode, specialityCodes]) => ({ stateCode, specialityCodes }));
          if (grouped.length) setLicenses(grouped.map((g) => ({ stateCode: g.stateCode, specialityCodes: g.specialityCodes })));
          setOriginalLicenseMap(idMap);
          setOriginalPairs(pairs);
        } catch (err) {
          console.error('Failed to load doctor licenses', err);
        }
      })();
    }
  }, [initialDoctor]);

  async function handleSubmit() {
    // password is only required when creating a new doctor
    if (!form.name || !form.email || !form.gender || (!initialDoctor && !form.password)) {
      alert("All fields are required");
      return;
    }

    // when creating a new doctor, require at least one license with state and at least one speciality
    if (!initialDoctor) {
      const hasAnyLicense = licenses.some((l) => (l.stateCode || '').trim() !== '' && (l.specialityCodes || []).length > 0);
      if (!hasAnyLicense) {
        alert('Please add at least one state license with one or more specialties before creating a doctor');
        return;
      }

      // additionally, ensure there are no partially filled license rows (state without specialties or specialties without state)
      for (const l of licenses) {
        const hasState = (l.stateCode || '').trim() !== '';
        const hasSpecs = (l.specialityCodes || []).length > 0;
        if ((hasState && !hasSpecs) || (!hasState && hasSpecs)) {
          alert('Please ensure each license row has both a State and at least one Specialty, or remove incomplete rows');
          return;
        }
      }
    }

  // prevent duplicate submissions
  if (isSubmitting) return;
  setIsSubmitting(true);

    const token = await auth.currentUser?.getIdToken();
    if (!token) {
      alert("Not authenticated");
      return;
    }

    try {
      if (initialDoctor) {
        const updated = await updateDoctor(token, initialDoctor.id, { name: form.name, gender: form.gender });

        // after updating doctor core fields, ensure any newly-added licenses are created
        for (const lic of licenses) {
          if (!lic.stateCode) continue;
          for (const spec of lic.specialityCodes) {
            const pair = `${lic.stateCode.toUpperCase()}|${spec.toUpperCase()}`;
            if (!originalPairs.has(pair)) {
              // create missing license on server
              try {
                await addDoctorLicense(token, initialDoctor.id, { stateCode: lic.stateCode, specialityCode: spec });
                // mark created so we don't duplicate if called again
                originalPairs.add(pair);
              } catch (err) {
                console.error("Failed to add license", err);
              }
            }
          }
        }

        onUpdated?.(updated as Doctor);
      } else {
        // create doctor via admin API
        // check duplicates by email (active doctors only). backend /api/admin/doctors returns only active doctors.
        try {
          const existing = (await getDoctors(token)) as Doctor[];
          const emailNorm = form.email.trim().toLowerCase();
          const duplicate = existing.some((d) => (d.email || '').trim().toLowerCase() === emailNorm);
          if (duplicate) {
            alert('A doctor with this email already exists. Please check active doctors before creating.');
            setIsSubmitting(false);
            return;
          }
        } catch (err) {
          console.warn('Failed to validate existing doctors before create, proceeding', err);
        }

        const created = (await createDoctor(token, { name: form.name, email: form.email, gender: form.gender, password: form.password, })) as Doctor;

        // for each license entry, post one license record per speciality selected
        for (const lic of licenses) {
          if (!lic.stateCode) continue;
          for (const spec of lic.specialityCodes) {
            await addDoctorLicense(token, created.id, { stateCode: lic.stateCode, specialityCode: spec });
          }
        }

        onCreated?.(created as Doctor);
      }
      onClose();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      // if backend returned 409 conflict, inform user that doctor exists
      if (msg.includes("API 409") || msg.includes("409")) {
        alert("Doctor already exists");
      } else {
        alert(msg || "Failed to save doctor");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDeleteLicense(idx: number) {
  const lic = licenses[idx];
  // if there are saved license ids for this state, deactivate them all; otherwise just remove locally
  const state = lic.stateCode?.toUpperCase() ?? "";
  const serverIds = originalLicenseMap[state] ?? [];
  if (serverIds.length === 0) {
    setLicenses((prev) => prev.filter((_, i) => i !== idx));
    return;
  }

  try {
    const token = await auth.currentUser?.getIdToken();
    if (!token) throw new Error("Not authenticated");

    // deactivate each saved license id for this state
    for (const id of serverIds) {
      await deactivateDoctorLicense(token, id);
    }

    // on success, remove the state row locally and clear original mapping
    setLicenses((prev) => prev.filter((_, i) => i !== idx));
    setOriginalLicenseMap((prev) => {
      const copy = { ...prev };
      delete copy[state];
      return copy;
    });
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : String(err);
    alert(msg || "Failed to delete license");
  }
}

  return (
    <div className={styles['modal-overlay']}>
      <div className={styles['modal-container']}>
        {/* Header */}
        <div className={styles['modal-header']}>
          <h2 className={styles['modal-title']}>{initialDoctor ? "Edit Doctor" : "Add Doctor"}</h2>
          <button onClick={onClose} aria-label="Close">✕</button>
        </div>

        {/* Body (scrollable) */}
        <div className={styles['modal-body']}>
          {/* Form */}
          <div className={styles['modal-section']}>
            <div className={styles['modal-row-grid']} style={{ marginBottom: 12 }}>
              <div>
                <label style={{ display: 'block', marginBottom: 6 }}>Full Name *</label>
                <input
                  className={styles['modal-input']}
                  placeholder="Doctor Name"
                    value={form.name}
                    disabled={!!initialDoctor}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                />
              </div>

              <div>
                <label style={{ display: 'block', marginBottom: 6 }}>Email *</label>
                <input
                  className={styles['modal-input']}
                  placeholder="Email"
                  value={form.email}
                  disabled={!!initialDoctor}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                />
              </div>
            </div>

                   {!initialDoctor && (
                     <div style={{ marginBottom: 12 }}>
                       <label style={{ display: 'block', marginBottom: 6 }}>Password *</label>
                       <input
                         className={styles['modal-input']}
                         placeholder="Password"
                         type="password"
                         value={form.password}
                         onChange={(e) => setForm({ ...form, password: e.target.value })}
                       />
                     </div>
                   )}

            <div style={{ marginBottom: 12 }}>
              <label style={{ display: 'block', marginBottom: 6 }}>Gender *</label>
              <select
                className={styles['modal-input']}
                value={form.gender}
                disabled={!!initialDoctor}
                onChange={(e) => setForm({ ...form, gender: e.target.value })}
                style={{ width: '100%' }}
              >
                <option value="">Select gender</option>
                <option value="MALE">Male</option>
                <option value="FEMALE">Female</option>
                <option value="OTHER">Other</option>
              </select>
            </div>
          </div>

          {/* State licenses section */}
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
            <h4 style={{ margin: 0 }}>State Licenses *</h4>
            <button className={styles.viewAppointmentsBtn} onClick={addStateLicense}>+ Add State</button>
          </div>

          <div className={styles['facility-info']}>
            <strong>Example:</strong>
            <div style={{ marginTop: 6 }}>
              Virginia license with multiple specialties like Cardiology and General Practice
            </div>
          </div>

          <div>
            {licenses.map((lic, idx) => (
              <div key={idx} className={styles['state-container']} style={{ position: 'relative' }}>
                {licenses.length > 1 && (
                  <button
                    type="button"
                    onClick={() => handleDeleteLicense(idx)}
                    aria-label="Delete license"
                    style={{
                      position: 'absolute',
                      right: 12,
                      top: 12,
                      background: 'transparent',
                      border: 'none',
                      cursor: 'pointer',
                      padding: 6,
                    }}
                  >
                    <Trash2 color="#ef4444" size={16} />
                  </button>
                )}
                <div style={{ marginBottom: 8 }}>
                  <label style={{ display: 'block', marginBottom: 6 }}>State *</label>
                  <select
                    className={styles['state-select']}
                    value={lic.stateCode}
                    onChange={(e) => {
                      const v = e.target.value;
                      setLicenses((prev) => prev.map((p, i) => (i === idx ? { ...p, stateCode: v } : p)));
                    }}
                  >
                    <option value="">Select state</option>
                    {states.map((s) => (
                      <option key={s.code} value={s.code}>{s.name}</option>
                    ))}
                  </select>
                </div>

                <div>
                  <label style={{ display: 'block', marginBottom: 8 }}>Specialties * (select multiple)</label>
                  <div className={styles['specialty-box']}>
                    <div className={styles['modal-grid']}>
                      {specialities.map((sp) => (
                        <label key={sp.code} className={styles['modal-checkbox']}>
                          <input
                            type="checkbox"
                            checked={lic.specialityCodes.includes(sp.code)}
                            onChange={() => toggleSpeciality(idx, sp.code)}
                          />
                          {sp.name}
                        </label>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
  </div>

  {/* Footer */}
        <div className={styles['modal-footer']}>
          <button className={styles.secondaryBtn} onClick={onClose}>
            Cancel
          </button>
          <button className={styles.viewAppointmentsBtn} onClick={handleSubmit}>
            {initialDoctor ? "Save Changes" : "Create Doctor"}
          </button>
        </div>
      </div>
    </div>
  );
}
