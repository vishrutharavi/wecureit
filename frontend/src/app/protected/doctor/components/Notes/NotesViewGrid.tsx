"use client";

import { useMemo, useState } from "react";
import CompletedAppointmentCard from "./CompletedAppointmentCard";
import styles from "../../doctor.module.scss";

function isSameLocalDay(a: Date, b: Date) {
	return (
		a.getFullYear() === b.getFullYear() &&
		a.getMonth() === b.getMonth() &&
		a.getDate() === b.getDate()
	);
}

const SAMPLE_DATA = [
	{
		patientName: "Sarah Wilson",
		ageGender: "47 years old • Female",
		date: "Friday, January 16, 2026",
		time: "13:00 - 13:45",
		duration: "45 min",
		complaint: "Post-surgery follow-up",
		facility: "Downtown Medical Center",
		status: "Completed",
		speciality: "Surgery",
	},
	{
		patientName: "John Smith",
		ageGender: "45 years old • Male",
		date: "Sunday, January 11, 2026",
		time: "10:00 - 10:30",
		duration: "30 min",
		complaint: "Follow-up consultation",
		facility: "Downtown Medical Center",
		status: "Cancelled",
		speciality: "General",
	},
	{
		patientName: "Jennifer Davis",
		ageGender: "51 years old • Female",
		date: "Tuesday, January 6, 2026",
		time: "09:00 - 09:30",
		duration: "30 min",
		complaint: "Palpitations and dizziness",
		facility: "Bethesda Health Center",
		status: "Completed",
		speciality: "Cardiology",
	},
	{
		patientName: "Robert Williams",
		ageGender: "58 years old • Male",
		date: "Saturday, December 27, 2025",
		time: "14:00 - 14:45",
		duration: "45 min",
		complaint: "Irregular heartbeat",
		facility: "Downtown Medical Center",
		status: "Completed",
		speciality: "Cardiology",
	},
];

export default function NotesGrid() {

	const [query, setQuery] = useState("");
	// empty string means no speciality selected (placeholder shown)
	const [speciality, setSpeciality] = useState("");

	// derive whether user is actively searching / filtering
	const isFiltering = Boolean(query.trim()) || Boolean(speciality);

	const visible = useMemo(() => {
		const today = new Date();
		// allow completed and cancelled to be visible
		const allowedStatuses = ["Completed", "Cancelled"];

		if (isFiltering) {
			const q = query.trim().toLowerCase();
			return SAMPLE_DATA.filter((s) => {
				if (!allowedStatuses.includes(s.status)) return false;
				if (speciality && s.speciality !== speciality) return false;
				if (!q) return true; // no query but speciality selected -> show all matching speciality
				return s.patientName.toLowerCase().includes(q);
			});
		}

		// default view: only local-time today appointments
		return SAMPLE_DATA.filter((s) => {
			if (!allowedStatuses.includes(s.status)) return false;
			// try to parse the human-readable date string - falls back to false if invalid
			const parsed = new Date(s.date);
			if (isNaN(parsed.getTime())) return false;
			return isSameLocalDay(parsed, today);
		});
	}, [query, speciality, isFiltering]);

	return (
		<div className={styles.scheduleContainer}>
			<h3>Completed Appointments & Clinical Notes</h3>
			<p className={styles.description}>Add clinical notes to completed appointments and view patient history</p>

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
						onChange={(e) => {
							// selecting 'Clear selection' will have the same empty value
							setSpeciality(e.target.value);
						}}
						className={styles.compactSelect}
					>
						<option value="">Select speciality</option>
						<option value="Cardiology">Cardiology</option>
						<option value="Surgery">Surgery</option>
						<option value="General">General</option>
						<option value="">Clear selection</option>
					</select>
				</div>
			</div>

			<div className={styles.gridTwo}>
				{visible.length ? (
					visible.map((s, idx) => <CompletedAppointmentCard key={idx} {...s} />)
				) : (
					<div className={styles.emptyCard}>No appointments found.</div>
				)}
			</div>
		</div>
	);
}
