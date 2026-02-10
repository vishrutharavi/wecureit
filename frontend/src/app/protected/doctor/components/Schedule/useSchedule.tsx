"use client";
import { useCallback, useEffect, useState } from "react";
import { apiFetch } from "../../../../../lib/api";

type ApiAppointment = {
  id: number | string;
  date?: string;
  duration?: number;
  patientId?: string;
  patientName?: string;
  startTime?: string;
  endTime?: string;
  facilityName?: string;
  roomNumber?: string;
  doctorName?: string;
  isActive?: boolean;
  chiefComplaints?: string;
    cancelledBy?: string;
  status?: string;
};

export type Appointment = {
  id: string;
  patientName: string;
  patientId?: string;
  start: string;
  end: string;
  status: "UPCOMING" | "CANCELLED" | "COMPLETED";
  facility?: string;
  room?: string;
  notes?: string;
  cancelledBy?: string;
};

export function useSchedule() {
  const [date, setDate] = useState<string>(() => new Date().toISOString().slice(0, 10));
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [breaks, setBreaks] = useState<Array<{ id: string; start: string; end: string; appointmentId?: string;}>>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const loadFor = useCallback(async (dt?: string) => {
    setLoading(true);
    setError(null);
    try {
      const raw = localStorage.getItem('doctorProfile');
      if (!raw) throw new Error('Not authenticated as doctor');
      const doc = JSON.parse(raw);
      const doctorId = doc.id;
      const token = localStorage.getItem('doctorToken') ?? undefined;
      const q = dt ?? date;
      const resp = await apiFetch(`/api/doctors/${doctorId}/schedule?date=${q}`, token);

      // Response might be legacy array of appointments (old) or an object { appointments, breaks }
      let appts: ApiAppointment[] = [];
      let breakRows: Array<{ id: string; startTime: string; endTime: string; appointmentId?: string; }> = [];
      if (Array.isArray(resp)) {
        appts = resp as ApiAppointment[];
        breakRows = [];
      } else if (resp && typeof resp === 'object') {
        const payload = resp as {
          appointments?: ApiAppointment[];
          breaks?: Array<{ id: string; startTime: string; endTime: string; appointmentId?: string }>;
        };
        appts = payload.appointments ?? [];
        breakRows = payload.breaks ?? [];
      }

      const mapped: Appointment[] = (appts as ApiAppointment[]).map(a => {
        const start = a.startTime ?? '';
        const end = a.endTime ?? '';
        // prefer explicit status returned by backend (COMPLETED/UPCOMING/CANCELLED); fallback to isActive
        const statusFromApi = (a as ApiAppointment).status;
        const status = statusFromApi ? statusFromApi.toUpperCase() as ('UPCOMING'|'CANCELLED'|'COMPLETED') : (a.isActive === undefined || a.isActive ? 'UPCOMING' : 'CANCELLED');
        return {
          id: String(a.id),
          patientName: a.patientName ?? a.patientId ?? 'Patient',
          patientId: a.patientId ? String(a.patientId) : undefined,
          start,
          end,
          status,
          // pass through cancelledBy actor (e.g. 'patient'|'doctor') when present
          cancelledBy: (a as ApiAppointment).cancelledBy ?? undefined,
          facility: a.facilityName,
          room: a.roomNumber,
          notes: a.chiefComplaints,
        };
      });
      setAppointments(mapped);

      const mappedBreaks = breakRows.map(b => ({ id: String(b.id), start: b.startTime, end: b.endTime, appointmentId: b.appointmentId }));
      setBreaks(mappedBreaks);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      setError(msg);
    } finally {
      setLoading(false);
    }
  }, [date]);

  useEffect(() => {
    loadFor(date);
  }, [date, loadFor]);

  return { date, setDate, appointments, breaks, loading, error, reload: () => loadFor() };
}
