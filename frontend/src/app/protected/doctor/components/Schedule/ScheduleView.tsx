"use client";
import { useMemo, useState, useEffect } from "react";
import styles from "../../doctor.module.scss";
import AppointmentModal from "./AppointmentModal";
import { useSchedule } from "./useSchedule";
import type { Appointment } from "./useSchedule";
import { apiFetch } from "../../../../../lib/api";
import { toLocalIso } from "../../../../../lib/dateUtils";

function formatShort(date: Date) {
	return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
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
			const [availabilityByDate, setAvailabilityByDate] = useState<Record<string, string | null>>({});
			const days = allDays.slice((weekIndex - 1) * 7, weekIndex * 7);

			useEffect(() => {
				// fetch facility name and availability for each visible date and cache it
				const load = async () => {
					try {
						const raw = localStorage.getItem('doctorProfile');
						if (!raw) return;
						const doc = JSON.parse(raw);
						const doctorId = doc.id;
						const token = localStorage.getItem('doctorToken') ?? undefined;
						const entries: Array<[string, string | null, boolean, string | null]> = [];
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
								}

								// Fetch availability data
								let availabilityText: string | null = null;
								try {
									const availResp = await apiFetch(`/api/doctors/${doctorId}/availability?from=${iso}&to=${iso}`, token);
									if (Array.isArray(availResp) && availResp.length > 0) {
										// Format availability times
										const timeRanges = availResp.map((avail: Record<string, unknown>) => {
											const start = avail.startTime as string;
											const end = avail.endTime as string;
											if (start && end) {
												// Format HH:mm to 12-hour format
												const formatTime = (time: string) => {
													const [h, m] = time.split(':').map(Number);
													const period = h >= 12 ? 'PM' : 'AM';
													const hour = h % 12 || 12;
													return `${hour}:${m.toString().padStart(2, '0')} ${period}`;
												};
												return `${formatTime(start)} - ${formatTime(end)}`;
											}
											return null;
										}).filter(Boolean);
										availabilityText = timeRanges.length > 0 ? timeRanges.join(', ') : null;
									}
								} catch (e) {
									console.warn('failed to fetch availability for', iso, e);
								}

								if (!hasAppts && !facility) {
									// no appointments present for this date
									entries.push([iso, null, false, availabilityText]);
									return;
								}
								entries.push([iso, facility, hasAppts, availabilityText]);
							} catch (e) {
								console.warn('failed to fetch schedule for', iso, e);
								entries.push([iso, null, false, null]);
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
						setAvailabilityByDate(prev => {
							const copy = { ...prev };
							for (const e of entries) {
								const k = e[0] as string;
								const v = e[3] as string | null;
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
					// Compare actual dates instead of using array index
					const today = new Date();
					const tomorrow = new Date(today);
					tomorrow.setDate(today.getDate() + 1);
					const isToday = toLocalIso(d) === toLocalIso(today);
					const isTomorrow = toLocalIso(d) === toLocalIso(tomorrow);

					return (
						<div key={idx} className={`${styles.dateCard} ${isToday ? ' ' + styles.today : ''}`}>
								<div className={styles.cardTitle}>{isToday ? 'Today' : isTomorrow ? 'Tomorrow' : d.toLocaleDateString(undefined, { weekday: 'short' })}</div>
							<div className={styles.cardDate}>{formatShort(d)}</div>

								<div style={{ marginTop: 12 }}>
									<div style={{ marginTop: 8, fontSize: 13, color: '#444', fontWeight: 600 }}>{(() => {
										const iso = toLocalIso(d);
										const facility = facilitiesByDate[iso];
										const has = hasAppointmentsByDate[iso];
										return has ? (facility ?? '') : '';
									})()}</div>
									<div style={{ marginTop: 4, fontSize: 12, color: '#666' }}>{(() => {
										const iso = toLocalIso(d);
										const availability = availabilityByDate[iso];
										return availability ?? '';
									})()}</div>
										<div style={{ marginTop: 12 }}>
											<button className={styles.viewAppointmentsBtn} onClick={() => {
												const iso = toLocalIso(d);
												// Store selected date in sessionStorage for the modal to use
												sessionStorage.setItem('selectedScheduleDate', iso);
												setDate(iso);
												setShowModal(true);
											}}>{'View appointments'}</button>
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
