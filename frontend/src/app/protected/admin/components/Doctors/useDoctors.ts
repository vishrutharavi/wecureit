"use client";

import { useEffect, useState } from "react";
import { getDoctors } from "@/lib/admin/adminApi";
import { auth } from "@/lib/firebase";
import { apiFetch } from "@/lib/api";
import type { Doctor, State, Speciality } from "../../types";

export function useDoctors() {
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    let mounted = true;
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (!mounted) return;
      if (!user) {
        setDoctors([]);
        setLoading(false);
        return;
      }

      try {
        const token = await user.getIdToken();
        const data = await getDoctors(token);
        if (mounted) setDoctors(data || []);
      } catch (err) {
        console.error("useDoctors: failed to load doctors", err);
        if (mounted) setDoctors([]);
      } finally {
        if (mounted) setLoading(false);
      }
    });

    return () => {
      mounted = false;
      unsubscribe();
    };
  }, []);

  return { doctors, loading };
}

export function useDoctorMeta() {
  const [states, setStates] = useState<State[]>([]);
  const [specialities, setSpecialities] = useState<Speciality[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function load() {
      const token = await auth.currentUser?.getIdToken();
      // if no token, still attempt unauthenticated admin endpoints? bail and clear loading
      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const [statesRes, specsRes] = await Promise.all([
          apiFetch("/api/admin/states", token),
          apiFetch("/api/admin/specialities", token),
        ]);

        setStates(statesRes || []);
        setSpecialities(specsRes || []);
      } catch {
        setStates([]);
        setSpecialities([]);
      } finally {
        setLoading(false);
      }
    }

    load();
  }, []);

  return { states, specialities, loading };
}

