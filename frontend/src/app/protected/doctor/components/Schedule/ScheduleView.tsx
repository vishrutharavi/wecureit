"use client";
import { useMemo, useState } from "react";
import styles from "../../doctor.module.scss";
import AppointmentModal from "./AppointmentModal";

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
			const days = allDays.slice((weekIndex - 1) * 7, weekIndex * 7);

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
								<div style={{ marginTop: 8, fontSize: 13, color: '#444' }}>{isToday ? 'Downtown Medical Center' : idx === 1 ? 'Bethesda Health Center' : ''}</div>
								<div style={{ marginTop: 12 }}>
									<button className={styles.viewAppointmentsBtn} onClick={() => setShowModal(true)}>View appointments</button>
								</div>
							</div>
						</div>
					);
				})}
			</div>
			{/* appointment modal - will list upcoming & cancelled appointments */}
			<AppointmentModal open={showModal} onClose={() => setShowModal(false)} />
		</div>
	);
}
