"use client";

import { useSearchParams, useRouter } from "next/navigation";
import styles from "../../doctor.module.scss";
import { ArrowLeft, MapPin, Calendar } from "lucide-react";

export default function ReferPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const patient = searchParams.get("patient") || "Patient";

  return (
    <div style={{ padding: '1rem 0' }}>
      <a style={{ display: 'inline-flex', alignItems: 'center', gap: 8, color: '#0b7', cursor: 'pointer' }} onClick={() => router.back()}>
        <ArrowLeft size={16} /> Back to Appointments
      </a>

      <div className={styles.scheduleContainer} style={{ marginTop: 16 }}>
        <h4 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 8 }}><MapPin size={16} /> Create Referral</h4>
        <p style={{ color: '#666', marginTop: 6 }}>Refer {patient} to a specialist</p>

        <div style={{ background: '#eefaf8', borderRadius: 8, padding: 18, marginTop: 12 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
            <div>
              <div style={{ color: '#666', fontSize: 13 }}>Patient</div>
              <div style={{ fontWeight: 800, marginTop: 6 }}>{patient}</div>
              <div style={{ color: '#666', marginTop: 6 }}>47 years old • Female</div>

              <div style={{ marginTop: 12 }}>
                <div style={{ color: '#666', fontSize: 13 }}>Chief Complaint</div>
                <div style={{ marginTop: 8 }}>Post-surgery follow-up</div>
              </div>
            </div>

            <div style={{ minWidth: 220 }}>
              <div style={{ color: '#666', fontSize: 13 }}>Original Appointment</div>
              <div style={{ marginTop: 6 }}><Calendar size={14} /> <span style={{ marginLeft: 8 }}>January 16, 2026</span></div>
              <div style={{ marginTop: 6 }}><MapPin size={14} /> <span style={{ marginLeft: 8 }}>Downtown Medical Center</span></div>
            </div>
          </div>
        </div>
      </div>

      <div className={styles.scheduleContainer} style={{ marginTop: 18 }}>
        <h4 style={{ marginTop: 0 }}>Select Specialty and State</h4>
        <p style={{ color: '#666', marginTop: 6 }}>Choose the specialty and state to view available doctors for referral</p>

        <div style={{ display: 'flex', gap: 12, marginTop: 12 }}>
          <div style={{ flex: 1 }}>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 6 }}>Specialty *</label>
            <select style={{ width: '100%', padding: '12px', borderRadius: 8, border: '1px solid #eef3f3' }}>
              <option value="">Select specialty...</option>
            </select>
          </div>

          <div style={{ width: 260 }}>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 6 }}>State *</label>
            <select style={{ width: '100%', padding: '12px', borderRadius: 8, border: '1px solid #eef3f3' }}>
              <option value="">Select state...</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  );
}
