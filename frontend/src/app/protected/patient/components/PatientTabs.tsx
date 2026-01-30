"use client";

import React from "react";
import styles from "../patient.module.scss";
import { Home, User, Calendar, CreditCard } from "lucide-react";

type Tab = "home" | "profile" | "appointments";

type Props = {
	active: Tab;
	onChange: (tab: Tab) => void;
};

export default function PatientTabs({ active, onChange }: Props) {
	return (
		<div className={styles.tabs}>
			<div className={styles.tabsLeftRow}>
				<button
					className={`${styles.tab} ${active === "home" ? styles.active : ""}`}
					onClick={() => onChange("home")}
				>
					<Home size={16} /> Home
				</button>

				<button
					className={`${styles.tab} ${active === "profile" ? styles.active : ""}`}
					onClick={() => onChange("profile")}
				>
					<User size={16} /> My Profile
				</button>

				<button
					className={`${styles.tab} ${active === "appointments" ? styles.active : ""}`}
					onClick={() => onChange("appointments")}
				>
					<Calendar size={16} /> Appointments
				</button>
					</div>

					<div className={styles.tabsRightActions}>
						<button className={styles.tab} onClick={() => onChange("appointments")}> 
							<CreditCard size={14} className={styles.iconSpacing} /> Payments
						</button>
					</div>
		</div>
	);
}

