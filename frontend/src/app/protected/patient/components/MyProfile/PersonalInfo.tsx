
"use client";

import React, { useState } from "react";
import styles from "../../patient.module.scss";

// Inline SectionCard and ReadField so PersonalInfo remains usable if Card.tsx is removed
export function ReadField({ children }: { children: React.ReactNode }) {
	return <div className={styles.readField}>{children}</div>;
}

type SectionCardProps = {
	icon?: React.ReactNode;
	title: string;
	subtitle?: string;
	children?: React.ReactNode;
};

function SectionCard({ icon, title, subtitle, children }: SectionCardProps) {
	return (
		<div className={`${styles.panelWhite} ${styles.profilePanel} ${styles.sectionCard}`}>
			<div className={styles.sectionHeader}>
				<div className={styles.sectionIcon}>{icon}</div>
				<div>
					<div className={styles.sectionTitle}>{title}</div>
					{subtitle ? <div className={styles.sectionSubtitle}>{subtitle}</div> : null}
				</div>
			</div>

			<div className={styles.cardContent}>{children}</div>
		</div>
	);
}
import { AiOutlineUser } from "react-icons/ai";
import { FiSave, FiEdit3 } from "react-icons/fi";

export default function PersonalInfo({
	name: initialName = "John Doe",
	sex: initialSex = "Male",
	dob: initialDob = "June 14, 1985",
	email: initialEmail = "john.doe@email.com",
	phone: initialPhone = "(555) 123-4567",
}: Partial<Record<string, string>> = {}) {
	const [editMode, setEditMode] = useState(false);
	// Initialize to server-safe value (avoids SSR/CSR hydration mismatch).
	const [email, setEmail] = useState<string>(initialEmail);

	// After mount, read real stored profile from localStorage and update state.
	React.useEffect(() => {
		try {
			if (typeof window === 'undefined') return;
			const raw = localStorage.getItem('patientProfile');
			if (raw) {
				const p = JSON.parse(raw);
				if (p?.email) setEmail(p.email);
			}
		} catch {}
	}, []);

	// PersonalInfo manages its own edit state locally. Do not dispatch or listen to global
	// patient:* events so editing this section doesn't affect other sections.


	function startEdit() {
		setEditMode(true);
	}

	function doSave() {
		try {
			const raw = localStorage.getItem("patientProfile");
			const p = raw ? JSON.parse(raw) : {};
			p.email = email;
			localStorage.setItem("patientProfile", JSON.stringify(p));
		} catch {}
		setEditMode(false);
	}

	function doCancel() {
		try {
			const raw = localStorage.getItem("patientProfile");
			if (raw) {
				const p = JSON.parse(raw);
				if (p?.email) setEmail(p.email);
			}
		} catch {}
		setEditMode(false);
	}

	return (
		<SectionCard icon={<AiOutlineUser size={18} />} title="Personal Information" subtitle="Your basic details">
				<div className={styles.sectionGrid}>
					<div className={styles.sectionActionsRightSpacing}>
						{!editMode ? (
							<button className={styles.editProfileBtn} onClick={startEdit}>
								<FiEdit3 className={styles.iconSpacing}/>Edit
							</button>
						) : (
							<div className={styles.actionRow}>
								<button onClick={doCancel} className={styles.cancelSecondary}>
									✕ Cancel
								</button>
								<button className={styles.viewAppointmentsBtn} onClick={doSave}>
									<FiSave className={styles.iconSpacing} /> Save Changes
								</button>
							</div>
						)}
					</div>

					<div>
						<div className={"fieldLabel"}>Full Name</div>
						<ReadField>{initialName}</ReadField>
					</div>

					<div className={styles.twoColGrid}>
						<div>
							<div className={"fieldLabel"}>Sex</div>
							<ReadField>{initialSex}</ReadField>
						</div>

						<div>
							<div className={"fieldLabel"}>Date of Birth</div>
							<ReadField>{initialDob}</ReadField>
						</div>
					</div>

					<div className={styles.twoColGrid}>
						<div>
							<div className={"fieldLabel"}>Email Address</div>
									{editMode ? (
										<input
											value={email}
											onChange={(e) => setEmail(e.target.value)}
											className={styles.inputField}
										/>
									) : (
										<ReadField>{email}</ReadField>
									)}
						</div>

						<div>
							<div className={"fieldLabel"}>Phone Number</div>
							<ReadField>{initialPhone}</ReadField>
						</div>
					</div>
				</div>
			</SectionCard>
	);
}
