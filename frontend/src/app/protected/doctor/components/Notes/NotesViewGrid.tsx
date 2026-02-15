"use client";

import { useMemo, useState, useEffect } from "react";
import CompletedAppointmentCard from "./CompletedAppointmentCard";
import styles from "../../doctor.module.scss";
import { apiFetch } from "../../../../../lib/api";
import { toLocalIso } from "../../../../../lib/dateUtils";

type UIAppointment = {
	patientName?: string;
	ageGender?: string;
	date?: string;
	time?: string;
	duration?: string;
	complaint?: string;
	facility?: string;
	status?: string;
	speciality?: string;
	appointmentDbId?: string;
	patientId?: string;
};

// Number of days to look back for completed appointments. Keep as a single constant
// so it's easy to change or wire to an env/config in future.
const LOOKBACK_DAYS = 14;

export default function NotesGrid() {

	const [query, setQuery] = useState("");
	// empty string means no speciality selected (placeholder shown)
	const [speciality, setSpeciality] = useState("");

		const [fetched, setFetched] = useState<Record<string, unknown>[]>([]);
		const [loading, setLoading] = useState<boolean>(false);
		const [error, setError] = useState<string | null>(null);

	// derive whether user is actively searching / filtering
	const isFiltering = Boolean(query.trim()) || Boolean(speciality);

	// derive list of specialties from fetched appointments (for the select dropdown)
	const specialties = useMemo(() => {
		const set = new Set<string>();
		for (const a of fetched) {
			const s = (a['specialityName'] as string) ?? (a['speciality'] as string);
			if (s) set.add(s);
		}
		return Array.from(set).sort();
	}, [fetched]);

	// Fetch completed/cancelled appointments for the last N days so notes can be added after completion
	useEffect(() => {
		let mounted = true;
		async function load() {
			setLoading(true);
			setError(null);
			try {
				const raw = localStorage.getItem('doctorProfile');
				if (!raw) throw new Error('Doctor not authenticated');
				const doc = JSON.parse(raw);
				const doctorId = doc.id;
				// fetch completed appointments in a single request (server reads appointment_history)
				const days = LOOKBACK_DAYS;
				const today = new Date();
				const end = toLocalIso(today);
				const startDate = new Date(today);
				startDate.setDate(today.getDate() - (days - 1));
				const start = toLocalIso(startDate);
				const res = await apiFetch(`/api/doctors/${doctorId}/completed-appointments?startDate=${start}&endDate=${end}`, localStorage.getItem('doctorToken') ?? undefined);
				const combined: Record<string, unknown>[] = [];
				if (res) {
					if (Array.isArray(res)) {
						combined.push(...(res as Record<string, unknown>[]));
					} else if (res && typeof res === 'object' && Array.isArray((res as Record<string, unknown>)['appointments'])) {
						combined.push(...((res as Record<string, unknown>)['appointments'] as Record<string, unknown>[]));
					}
				}
				if (mounted) setFetched(combined);
			} catch (err) {
				setError(err instanceof Error ? err.message : String(err));
				setFetched([]);
			} finally {
				if (mounted) setLoading(false);
			}
		}
		load();
		return () => { mounted = false; };
	}, []);

	const visible = useMemo(() => {
		// allow only completed appointments to be visible in the Clinical Notes view
		const allowedStatuses = ["COMPLETED", "Completed"];

		// normalize fetched entries to same shape used by SAMPLE_DATA for UI
			const normalized: UIAppointment[] = fetched.map((a: Record<string, unknown>) => {
				const rawDate = a['date'] as string | undefined;
				const rawStart = a['startTime'] as string | undefined;
				const rawEnd = a['endTime'] as string | undefined;
				const dateLabel = rawDate ? new Date(rawDate).toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' }) : (rawStart ? new Date(rawStart).toLocaleDateString() : '');
				const timeLabel = rawStart && rawEnd ? `${new Date(rawStart).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })} - ${new Date(rawEnd).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` : '';
				return {
					patientName: (a['patientName'] as string) ?? (a['patientId'] as string) ?? 'Patient',
					ageGender: a['ageGender'] as string | undefined,
					date: dateLabel,
					time: timeLabel,
					duration: a['duration'] ? `${String(a['duration'])} min` : undefined,
					complaint: a['chiefComplaints'] as string | undefined,
					facility: a['facilityName'] as string | undefined,
					status: (a['status'] as string) ?? ((a['isActive'] as boolean) ? 'UPCOMING' : 'CANCELLED'),
					speciality: (a['specialityName'] as string) ?? (a['speciality'] as string) ?? undefined,
					appointmentDbId: a['id'] ? String(a['id']) : undefined,
					patientId: a['patientId'] ? String(a['patientId']) : undefined,
				};
			});

	const source: UIAppointment[] = normalized;

			if (isFiltering) {
				const q = query.trim().toLowerCase();
						return source.filter((s: UIAppointment) => {
							const st = s.status;
							if (!st || !allowedStatuses.includes(st)) return false;
							if (speciality && s.speciality !== speciality) return false;
							if (!q) return true;
							const pname = (s.patientName ?? '') as string;
							const comp = (s.complaint ?? '') as string;
							return pname.toLowerCase().includes(q) || comp.toLowerCase().includes(q);
						});
			}

					// default: show recent completed/cancelled appointments (from normalized source)
					return source.filter((s: UIAppointment) => {
						const st = s.status;
						return !!st && allowedStatuses.includes(st);
					});
	}, [fetched, query, speciality, isFiltering]);

	return (
		<div className={styles.scheduleContainer}>
			<h3>Completed Appointments & Clinical Notes</h3>
			<p className={styles.description}>Add clinical notes to completed appointments and view patient history</p>
			{loading ? (
				<div className={styles.emptyCard}>Loading appointments…</div>
			) : error ? (
				<div className={styles.emptyCard}>Error loading appointments: {error}</div>
			) : null}
			<div className={styles.scheduleHeader} style={{ marginBottom: 12 }}>
				<div style={{ flex: 1, marginRight: 12 }}>
					<input
						aria-label="Search patients"
						placeholder="Search patient name or notes..."
						value={query}
						onChange={(e) => setQuery(e.target.value)}
						className={styles.compactSelect}
						style={{ width: "100%" }}
					/>
				</div>

				<div style={{ width: 200 }}>
					<select
						value={speciality}
						onChange={(e) => setSpeciality(e.target.value)}
						className={styles.compactSelect}
					>
						<option value="">All specialties</option>
						{/** derive specialties dynamically from fetched data */}
						{(specialties || []).map((s) => (
							<option key={s} value={s}>{s}</option>
						))}
					</select>
				</div>
			</div>

			<div className={styles.gridTwo}>
				{visible.length ? (
					visible.map((s, idx) => (
						<CompletedAppointmentCard
							key={idx}
							patientName={s.patientName ?? 'Patient'}
							ageGender={s.ageGender}
							date={s.date ?? ''}
							time={s.time ?? ''}
							duration={s.duration}
							complaint={s.complaint}
							facility={s.facility}
							status={s.status}
							appointmentDbId={s.appointmentDbId}
							patientId={s.patientId}
						/>
					))
				) : (
					<div className={styles.emptyCard}>No appointments found.</div>
				)}
			</div>
		</div>
	);
}
