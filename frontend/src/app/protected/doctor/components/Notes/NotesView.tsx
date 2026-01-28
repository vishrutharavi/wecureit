"use client";

import CompletedAppointmentCard from "./CompletedAppointmentCard";
import styles from "../../doctor.module.scss";

export default function NotesView() {
	const sample = [
		{
			patientName: "Sarah Wilson",
			ageGender: "47 years old • Female",
			date: "Friday, January 16, 2026",
			time: "13:00 - 13:45",
			duration: "45 min",
			complaint: "Post-surgery follow-up",
			facility: "Downtown Medical Center",
			status: "Completed",
		},
		{
			patientName: "John Smith",
			ageGender: "45 years old • Male",
			date: "Sunday, January 11, 2026",
			time: "10:00 - 10:30",
			duration: "30 min",
			complaint: "Follow-up consultation",
			facility: "Downtown Medical Center",
			status: "Completed",
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
		},
	];

	return (
		<div className={styles.scheduleContainer}>
			<h3>Completed Appointments & Clinical Notes</h3>
			<p style={{ color: '#666', marginTop: 6 }}>Add clinical notes to completed appointments and view patient history</p>

			<div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 20, marginTop: 18 }}>
				{sample.map((s, idx) => (
					<CompletedAppointmentCard key={idx} {...s} />
				))}
			</div>
		</div>
	);
}
