"use client";
import { useMemo, useState, useEffect } from "react";
import styles from "../../doctor.module.scss";
import AppointmentModal from "./AppointmentModal";
import { useSchedule } from "./useSchedule";
import type { Appointment } from "./useSchedule";
import { apiFetch } from "../../../../../lib/api";

function formatShort(date: Date) {
	return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

function toLocalIso(d: Date) {
	const y = d.getFullYear();
	const m = String(d.getMonth() + 1).padStart(2, '0');
	const day = String(d.getDate()).padStart(2, '0');
	return `${y}-${m}-${day}`;
}

export default function ScheduleView() {
			const allDays = useMemo(() => {
				const base = new Date();
				return Array.from({ length: 14 }).map((_, i) => {
					const d = new Date(base);
					d.setDate(base.getDate() + i);
					return d;
				});
			}, []);

			const [weekIndex, setWeekIndex] = useState<number>(1); // show Week 2 by default like screenshot
			const [showModal, setShowModal] = useState<boolean>(false);
			const { appointments, setDate } = useSchedule();
			const [facilitiesByDate, setFacilitiesByDate] = useState<Record<string, string | null>>({});
			const [hasAppointmentsByDate, setHasAppointmentsByDate] = useState<Record<string, boolean>>({});
			const days = allDays.slice((weekIndex - 1) * 7, weekIndex * 7);

			useEffect(() => {
				// fetch facility name for each visible date and cache it
				const load = async () => {
					try {
						const raw = localStorage.getItem('doctorProfile');
						if (!raw) return;
						const doc = JSON.parse(raw);
						const doctorId = doc.id;
						const token = localStorage.getItem('doctorToken') ?? undefined;
						const entries: Array<[string, string | null, boolean]> = [];
						const visibleDays = allDays.slice((weekIndex - 1) * 7, weekIndex * 7);
						await Promise.all(visibleDays.map(async (d) => {
							const iso = toLocalIso(d);
							try {
								const resp = await apiFetch(`/api/doctors/${doctorId}/schedule?date=${iso}`, token);
								let facility: string | null = null;
								let hasAppts = false;
								if (Array.isArray(resp) && resp.length) {
									facility = resp[0].facilityName ?? null;
									// consider only non-cancelled / upcoming appts as active
									hasAppts = resp.some((r: Record<string, unknown>) => {
										try {
											const sRaw = r.status ?? '';
											const s = String(sRaw).toUpperCase();
											if (s) return s !== 'CANCELLED';
											const isActiveRaw = r.isActive as unknown;
											return isActiveRaw === undefined || isActiveRaw === true;
										} catch { return false; }
									});
								} else if (resp && typeof resp === 'object' && Array.isArray(resp.appointments) && resp.appointments.length) {
									facility = resp.appointments[0].facilityName ?? null;
									hasAppts = resp.appointments.some((r: Record<string, unknown>) => {
										try {
											const sRaw = r.status ?? '';
											const s = String(sRaw).toUpperCase();
											if (s) return s !== 'CANCELLED';
											const isActiveRaw = r.isActive as unknown;
											return isActiveRaw === undefined || isActiveRaw === true;
										} catch { return false; }
									});
								} else {
									// no appointments present for this date
									entries.push([iso, null, false]);
									return;
								}
								entries.push([iso, facility, hasAppts]);
							} catch (e) {
								console.warn('failed to fetch schedule for', iso, e);
								entries.push([iso, null, false]);
							}
						}));
						setFacilitiesByDate(prev => {
							const copy = { ...prev };
							for (const e of entries) {
								const k = e[0] as string;
								const v = e[1] as string | null;
								copy[k] = v;
							}
							return copy;
						});
						setHasAppointmentsByDate(prev => {
							const copy = { ...prev };
							for (const e of entries) {
								const k = e[0] as string;
								const v = e[2] as boolean;
								copy[k] = v;
							}
							return copy;
						});
					} catch (err) {
						console.warn('facility load error', err);
					}
				};
				load();
			}, [weekIndex, allDays]);

	return (
		<div className={styles.scheduleContainer}>
					<div className={styles.scheduleHeader}>
						<h3 style={{ margin: 0 }}>My Schedule - Next 2 Weeks</h3>
						<div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
							<div className={styles.weekSwitch}>
								<button className={`${styles.weekBtn} ${weekIndex === 1 ? ' ' + styles.active : ''}`} onClick={() => setWeekIndex(1)}>‹ Week 1</button>
								<button className={`${styles.weekBtn} ${weekIndex === 2 ? ' ' + styles.active : ''}`} onClick={() => setWeekIndex(2)}>Week 2 ›</button>
							</div>
						</div>
					</div>

			<div className={styles.dateGrid}>
				{days.map((d, idx) => {
					const isToday = idx === 0;
					return (
						<div key={idx} className={`${styles.dateCard} ${isToday ? ' ' + styles.today : ''}`}>
								<div className={styles.cardTitle}>{isToday ? 'Today' : idx === 1 ? 'Tomorrow' : d.toLocaleDateString(undefined, { weekday: 'short' })}</div>
							<div className={styles.cardDate}>{formatShort(d)}</div>

								<div style={{ marginTop: 12 }}>
									<div style={{ marginTop: 8, fontSize: 13, color: '#444' }}>{(() => {
										const iso = toLocalIso(d);
										const facility = facilitiesByDate[iso];
										const has = hasAppointmentsByDate[iso];
										return has ? (facility ?? '') : '';
									})()}</div>
										<div style={{ marginTop: 12 }}>
											<button className={styles.viewAppointmentsBtn} onClick={() => { const iso = d.toISOString().slice(0,10); setDate(iso); setShowModal(true); }}>{'View appointments'}</button>
										</div>
								</div>
						</div>
					);
				})}
			</div>
				{/* appointment modal - will list upcoming & cancelled appointments */}
				<AppointmentModal open={showModal} onClose={() => setShowModal(false)} appointments={appointments as Appointment[]} />
		</div>
	);
}
