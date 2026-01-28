"use client";

import React from "react";
import styles from "../doctor.module.scss";
import { Calendar, Clock, FileText, Share2, Mail } from "lucide-react";
import ReferralModal from "./Referral/ReferralModal";
import MessageModal from "./Message/MessageModal";

type Tab = "schedule" | "availability" | "notes";

type Props = {
  active: Tab;
  onChange: (tab: Tab) => void;
};

export default function DoctorTabs({ active, onChange }: Props) {
  const [showReferrals, setShowReferrals] = React.useState(false);
  const [showMessages, setShowMessages] = React.useState(false);

  // sample referrals; in future this should come from an API
  const sampleReferrals = [
    {
      id: 'r1',
      referringDoctor: 'Dr. Alice Park',
      referringDoctorEmail: 'alice.park@example.com',
      facility: 'Downtown Medical Center',
      patient: 'Sarah Wilson',
      speciality: 'Cardiology',
      date: '2026-01-18',
    },
    {
      id: 'r2',
      referringDoctor: 'Dr. Bob Lee',
      referringDoctorEmail: 'bob.lee@example.com',
      facility: 'Bethesda Health Center',
      patient: 'John Smith',
      speciality: 'Orthopedics',
      date: '2026-01-12',
    },
  ];

  return (
    <div className={styles.tabs}>
      <div style={{ display: 'flex', gap: 8 }}>
        <button
          className={`${styles.tab} ${active === "schedule" ? styles.active : ""}`}
          onClick={() => onChange("schedule")}
        >
          <Calendar size={16} /> My Schedule
        </button>

        <button
          className={`${styles.tab} ${active === "availability" ? styles.active : ""}`}
          onClick={() => onChange("availability")}
        >
          <Clock size={16} /> Set Availability
        </button>

        <button
          className={`${styles.tab} ${active === "notes" ? styles.active : ""}`}
          onClick={() => onChange("notes")}
        >
          <FileText size={16} /> Appointments & Notes
        </button>
      </div>

      <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button className={`${styles.tab} ${showMessages ? styles.active : ''}`} onClick={() => setShowMessages(true)} title="Messages">
          <Mail size={14} style={{ marginRight: 8 }} /> Messages
        </button>

        <button className={`${styles.tab} ${showReferrals ? styles.active : ''}`} onClick={() => setShowReferrals(true)} title="View Referrals">
          <Share2 size={14} style={{ marginRight: 8 }} /> Referrals {sampleReferrals.length ? <span style={{ marginLeft: 6 }} className={styles.smallBadge}>{sampleReferrals.length}</span> : null}
        </button>
      </div>

      <ReferralModal open={showReferrals} onClose={() => setShowReferrals(false)} items={sampleReferrals} />
      <MessageModal open={showMessages} onClose={() => setShowMessages(false)} />
    </div>
  );
}

