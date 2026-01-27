import { Pencil, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import styles from "../../admin.module.scss";
import type { Doctor, State, Speciality } from "../../types";
import { auth } from "@/lib/firebase";
import { getDoctorLicenses } from "@/lib/admin/adminApi";

export default function DoctorCard({
  doctor,
  onEdit,
  onDelete,
  metaStates,
  metaSpecialities,
}: {
  doctor: Doctor;
  onEdit?: (doctor: Doctor) => void;
  onDelete?: (doctor: Doctor) => void;
  metaStates?: State[];
  metaSpecialities?: Speciality[];
}) {
  type DoctorLicense = {
    id: string;
    doctorId: string;
    stateCode?: string;
    specialityCode?: string;
    speciality?: string;
    isActive?: boolean;
    stateName?: string;
    specialityName?: string;
  };

  type StateGroup = { code: string; name: string; specialties: string[] };
  const [stateGroups, setStateGroups] = useState<StateGroup[]>([]);

  useEffect(() => {
    let mounted = true;
    async function load() {
      const user = auth.currentUser;
      if (!user) return;

      try {
        const token = await user.getIdToken();
        const data = (await getDoctorLicenses(token, doctor.id)) as DoctorLicense[];
        // keep only active licenses
        const active = (data || []).filter((r) => r?.isActive !== false);

        const map = new Map<string, Set<string>>();
        const findSpecName = (code: string) => {
          if (!code) return code;
          // exact match
          let found = metaSpecialities?.find((m) => m.code === code);
          if (found) return found.name;
          // try uppercase/lowercase variants
          found = metaSpecialities?.find((m) => m.code === code.toUpperCase());
          if (found) return found.name;
          found = metaSpecialities?.find((m) => m.code === code.toLowerCase());
          if (found) return found.name;
          // try match by name (if code is actually the name)
          found = metaSpecialities?.find((m) => m.name === code);
          if (found) return found.name;
          return code;
        };

        const stateNameMap = new Map<string, string>();
        for (const row of active) {
          const sc = row.stateCode ?? "";
          // prefer backend-provided specialityName, then 'speciality'/'specialityCode'
          const specCode = row.speciality ?? row.specialityCode ?? "";
          const displaySpec = row.specialityName ?? findSpecName(specCode);
          // capture stateName from backend when available
          if (row.stateName) stateNameMap.set(sc, row.stateName);
          if (!map.has(sc)) map.set(sc, new Set<string>());
          if (displaySpec) map.get(sc)?.add(displaySpec);
        }

        const findStateName = (code: string) => {
          if (!code) return code;
          let found = metaStates?.find((s) => s.code === code);
          if (found) return found.name;
          found = metaStates?.find((s) => s.code === code.toUpperCase());
          if (found) return found.name;
          found = metaStates?.find((s) => s.code === code.toLowerCase());
          if (found) return found.name;
          found = metaStates?.find((s) => s.name === code);
          if (found) return found.name;
          return code;
        };

        const groups: StateGroup[] = Array.from(map.entries()).map(([code, set]) => {
          const name = stateNameMap.get(code) ?? findStateName(code);
          return { code, name, specialties: Array.from(set) };
        });

  // sort by name for deterministic ordering
        groups.sort((a, b) => a.name.localeCompare(b.name));
  // debug: warn if any speciality names still look like codes (all-uppercase)
  const anyCodes = groups.some((g) => g.specialties.some((s) => /^[A-Z0-9_]{2,}$/.test(s)));
  if (anyCodes) console.debug("DoctorCard: some speciality names look like codes; metaSpecialities may be missing or mismatched", { doctorId: doctor.id, groups });
        if (mounted) setStateGroups(groups);
      } catch (e) {
        // ignore and leave groups empty
        console.error("DoctorCard: failed to load licenses", e);
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, [doctor, metaStates, metaSpecialities]);

  return (
    <div className={styles.itemCard}>
      <div className={styles.itemCardHeader}>
        <div>
          <h3 className={styles.itemCardTitle}>{doctor?.name ?? "Unnamed"}</h3>
          <p className={styles.itemCardEmail}>{doctor?.email ?? ""}</p>
        </div>

        <div className={styles.itemActions}>
          <Pencil
            size={18}
            style={{ cursor: onEdit ? "pointer" : "default" }}
            onClick={() => onEdit?.(doctor)}
          />
          <Trash2
            size={18}
            className={styles.delete}
            style={{ cursor: onDelete ? "pointer" : "default" }}
            onClick={() => onDelete?.(doctor)}
          />
        </div>
      </div>

      {/* show state-level licenses grouped by state: "State Name: specialty1, specialty2" */}
      <div style={{ marginTop: 12 }}>
        <div style={{ marginBottom: 8, fontWeight: 700, color: '#b91c1c' }}>Licenses</div>
        <div style={{ display: 'flex', gap: 12, flexDirection: 'column' }}>
        {(stateGroups ?? []).map((g) => (
          <div
            key={g.code}
            style={{
              background: "#fee2e2",
              color: "#991b1b",
              borderRadius: 12,
              padding: "12px 16px",
              display: "flex",
              flexDirection: 'column',
              gap: 8,
              fontSize: 13,
              width: '100%',
              boxSizing: 'border-box',
            }}
          >
            <div style={{ fontWeight: 700 }}>{g.name}:</div>
            <div style={{ display: "flex", gap: 8, flexWrap: 'wrap' }}>
              {g.specialties.map((s) => (
                <span
                  key={s}
                  style={{
                    background: "#fff",
                    color: "#374151",
                    borderRadius: 9999,
                    padding: "6px 10px",
                    fontSize: 13,
                    whiteSpace: "nowrap",
                  }}
                >
                  {s}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
      </div>

      <div className={styles.badges} style={{ marginTop: 12 }}>
        {(doctor?.specialties ?? []).map((s: string) => {
          const name = metaSpecialities?.find((m) => m.code === s)?.name ?? s;
          return (
            <span key={s} className={styles.badge}>
              {name}
            </span>
          );
        })}
      </div>
    </div>
  );
}
