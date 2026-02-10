
"use client";

import React, { useState } from "react";
import { apiFetch } from '@/lib/api';
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
	name: initialName = "",
	sex: initialSex = "",
	dob: initialDob = "",
	email: initialEmail = "",
	phone: initialPhone = "",
}: Partial<Record<string, string>> = {}) {
	const [editMode, setEditMode] = useState(false);
	// Initialize to server-safe value (avoids SSR/CSR hydration mismatch).
	const [email, setEmail] = useState<string>(initialEmail);
	const [name, setName] = useState<string>(initialName);
	const [sex, setSex] = useState<string>(initialSex);
	const [dob, setDob] = useState<string>(initialDob);
	const [phone, setPhone] = useState<string>(initialPhone);

	// After mount, read real stored profile from localStorage and update state.
	React.useEffect(() => {
		try {
			if (typeof window === 'undefined') return;
			const raw = localStorage.getItem('patientProfile');
			if (raw) {
				const p = JSON.parse(raw);
				// If user has edited email in portal, prefer secondaryEmail for display/editing.
				if (p?.secondaryEmail) setEmail(p.secondaryEmail);
				else if (p?.email) setEmail(p.email);

				// populate other profile fields if present
				if (p?.name) setName(p.name);
				if (p?.sex) setSex(p.sex);
				if (p?.dob) setDob(p.dob);
				if (p?.phone) setPhone(p.phone);
			}

			// If any of the important fields are missing, attempt to refresh from the server
			(async () => {
				try {
					const raw2 = localStorage.getItem('patientProfile');
					const p2 = raw2 ? JSON.parse(raw2) : {};
					if (!p2 || !p2.phone || !p2.dob || !p2.sex) {
						const me = await apiFetch('/api/patient/me');
						if (me) {
								try {
									const merged = { ...(p2 || {}), ...(me || {}) };
									localStorage.setItem('patientProfile', JSON.stringify(merged));
									if (me?.phone) setPhone(me.phone);
									if (me?.dob) setDob(me.dob);
									if (me?.sex) setSex(me.sex);
									if (me?.name) setName(me.name);
									if (me?.secondaryEmail) setEmail(me.secondaryEmail);
								} catch {}
						}
					}
				} catch {}
			})();
		} catch {}
	}, []);

	// PersonalInfo manages its own edit state locally. Do not dispatch or listen to global
	// patient:* events so editing this section doesn't affect other sections.


	function startEdit() {
		setEditMode(true);
	}

		async function doSave() {
			try {
				// call backend to persist profile changes; edited email goes to secondaryEmail
				try {
					const payload: { email?: string; name?: string; phone?: string } = { email, name, phone };
					const res = await fetch('/api/patient/profile', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
					if (res.ok) {
						const updated = await res.json();
						try {
							const raw = localStorage.getItem('patientProfile');
							const p = raw ? JSON.parse(raw) : {};
							// merge returned fields
							p.email = updated.email ?? p.email;
							p.secondaryEmail = updated.secondaryEmail ?? p.secondaryEmail;
							p.name = updated.name ?? p.name ?? name;
							p.phone = updated.phone ?? p.phone ?? phone;
							p.city = updated.city ?? p.city;
							p.state = updated.state ?? p.state;
							p.zip = updated.zip ?? p.zip;
							p.address = updated.address ?? p.address;
							localStorage.setItem('patientProfile', JSON.stringify(p));
						} catch {}
					} else {
						// fallback: store locally if backend update fails
						const raw = localStorage.getItem('patientProfile');
						const p = raw ? JSON.parse(raw) : {};
						p.secondaryEmail = email;
						p.name = name;
						p.phone = phone;
						localStorage.setItem('patientProfile', JSON.stringify(p));
					}
				} catch {
					// network error: persist locally
					const raw = localStorage.getItem('patientProfile');
					const p = raw ? JSON.parse(raw) : {};
					p.secondaryEmail = email;
					p.name = name;
					p.phone = phone;
					localStorage.setItem('patientProfile', JSON.stringify(p));
				}
			} catch {}
			setEditMode(false);
		}

	function doCancel() {
		try {
			const raw = localStorage.getItem("patientProfile");
			if (raw) {
				const p = JSON.parse(raw);
				if (p?.email) setEmail(p.email);
				if (p?.name) setName(p.name);
				if (p?.sex) setSex(p.sex);
				if (p?.dob) setDob(p.dob);
				if (p?.phone) setPhone(p.phone);
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
						<ReadField>{name}</ReadField>
					</div>

					<div className={styles.twoColGrid}>
						<div>
							<div className={"fieldLabel"}>Sex</div>
							<ReadField>{sex}</ReadField>
						</div>

						<div>
							<div className={"fieldLabel"}>Date of Birth</div>
							<ReadField>{dob}</ReadField>
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
								{editMode ? (
									<div style={{ fontSize: 12, color: '#6b7280', marginTop: 6 }}>
										Note: updating this email will save it as your secondary contact email and will not change your login email.
									</div>
								) : null}
						</div>

						<div>
							<div className={"fieldLabel"}>Phone Number</div>
							{editMode ? (
								<input value={phone} onChange={(e) => setPhone(e.target.value)} className={styles.inputField} />
							) : (
								<ReadField>{phone}</ReadField>
							)}
						</div>
					</div>
				</div>
			</SectionCard>
	);
}
