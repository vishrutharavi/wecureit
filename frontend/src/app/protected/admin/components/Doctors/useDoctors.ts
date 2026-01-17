// src/app/protected/admin/components/Doctors/useDoctors.ts

import { useEffect, useState } from "react";
import { apiFetch } from "@/lib/api";
import { State, Speciality } from "../../types";

export function useDoctorMeta() {
  const [states, setStates] = useState<State[]>([]);
  const [specialities, setSpecialities] = useState<Speciality[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function load() {
      try {
        const [statesRes, specsRes] = await Promise.all([
          apiFetch("/api/admin/states"),
          apiFetch("/api/admin/specialities"),
        ]);

        setStates(statesRes || []);
        setSpecialities(specsRes || []);
      } catch (err) {
        // log the error so it's visible during debugging and allow the modal to render
        // (previously an uncaught rejection would keep `loading` true forever)
        console.error("Failed to load doctor metadata:", err);
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  return { states, specialities, loading };
}
