"use client";

import React, { useEffect, useState, useCallback } from "react";
import { auth } from "@/lib/firebase";
import { getBottleneckReport, triggerGraphSync } from "@/lib/admin/adminApi";

type DoctorBottleneck = {
  doctorId: string; doctorName: string;
  pendingReferrals: number; acceptedReferrals: number; severity: string;
};
type SpecialityImbalance = {
  specialityCode: string; specialityName: string;
  totalReferrals: number; completedReferrals: number; completionRate: number;
};
type CrossStateWarning = {
  referralId: string; fromDoctorName: string; toDoctorName: string;
  patientName: string; patientState: string; toDoctorState: string;
};
type BottleneckReport = {
  overloadedSpecialists: DoctorBottleneck[];
  specialityImbalances: SpecialityImbalance[];
  crossStateWarnings: CrossStateWarning[];
  generatedAt: string;
};

const SEVERITY_COLOR: Record<string, string> = {
  HIGH:   "#ef4444",
  MEDIUM: "#f97316",
  LOW:    "#eab308",
};

function SectionHeader({ title, count }: { title: string; count: number }) {
  return (
    <div style={{ display: "flex", alignItems: "center", gap: "0.75rem", marginBottom: "1rem" }}>
      <h3 style={{ margin: 0, fontSize: "1rem", fontWeight: 700, color: "#7f1d1d" }}>{title}</h3>
      <span style={{
        background: "#fee2e2", color: "#ef4444", borderRadius: 999,
        padding: "0.1rem 0.6rem", fontSize: "0.8rem", fontWeight: 700,
      }}>{count}</span>
    </div>
  );
}

export default function AlertsDashboard() {
  const [report, setReport]   = useState<BottleneckReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);

  const fetchReport = useCallback(async () => {
    const user = auth.currentUser;
    if (!user) return;
    try {
      setLoading(true);
      const token = await user.getIdToken();
      const data = await getBottleneckReport(token);
      if (data) setReport(data as BottleneckReport);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchReport(); }, [fetchReport]);

  async function handleSync() {
    const user = auth.currentUser;
    if (!user) return;
    try {
      setSyncing(true);
      const token = await user.getIdToken();
      await triggerGraphSync(token);
      await fetchReport();
    } catch { /* ignore */ }
    finally { setSyncing(false); }
  }

  if (loading) {
    return <div style={{ textAlign: "center", padding: "4rem", color: "#9ca3af" }}>Loading alerts…</div>;
  }

  const specialists = report?.overloadedSpecialists ?? [];
  const imbalances  = report?.specialityImbalances   ?? [];
  const warnings    = report?.crossStateWarnings      ?? [];

  return (
    <div style={{ marginTop: "2rem" }}>
      {/* Header with actions */}
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.5rem" }}>
        <div>
          <h2 style={{ margin: 0, fontSize: "1.4rem", fontWeight: 800, color: "#7f1d1d" }}>Referral Alerts</h2>
          {report?.generatedAt && (
            <div style={{ fontSize: "0.8rem", color: "#9ca3af", marginTop: 4 }}>
              Generated {new Date(report.generatedAt).toLocaleString()}
            </div>
          )}
        </div>
        <div style={{ display: "flex", gap: "0.75rem" }}>
          <button
            onClick={fetchReport}
            style={{ padding: "0.5rem 1rem", borderRadius: 8, border: "1px solid #fecaca", background: "white", color: "#ef4444", cursor: "pointer", fontWeight: 600 }}
          >
            Refresh
          </button>
          <button
            onClick={handleSync}
            disabled={syncing}
            style={{ padding: "0.5rem 1rem", borderRadius: 8, border: "none", background: "linear-gradient(135deg,#ef4444,#f43f5e)", color: "white", cursor: "pointer", fontWeight: 600 }}
          >
            {syncing ? "Syncing…" : "Sync Graph"}
          </button>
        </div>
      </div>

      {/* ── Section 1: Overloaded Specialists ── */}
      <div style={{ background: "white", borderRadius: 12, padding: "1.5rem", border: "1px solid #fecaca", marginBottom: "1.5rem" }}>
        <SectionHeader title="Overloaded Specialists" count={specialists.length} />
        {specialists.length === 0 ? (
          <div style={{ color: "#9ca3af", fontSize: "0.9rem" }}>No overloaded specialists detected.</div>
        ) : (
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.875rem" }}>
            <thead>
              <tr style={{ color: "#9ca3af", textAlign: "left", borderBottom: "1px solid #f3f4f6" }}>
                <th style={{ padding: "0.4rem 0.5rem" }}>Doctor</th>
                <th style={{ padding: "0.4rem 0.5rem" }}>Pending</th>
                <th style={{ padding: "0.4rem 0.5rem" }}>Accepted</th>
                <th style={{ padding: "0.4rem 0.5rem" }}>Severity</th>
              </tr>
            </thead>
            <tbody>
              {specialists.map((s) => (
                <tr key={s.doctorId} style={{ borderBottom: "1px solid #f9fafb" }}>
                  <td style={{ padding: "0.6rem 0.5rem", fontWeight: 600, color: "#374151" }}>{s.doctorName}</td>
                  <td style={{ padding: "0.6rem 0.5rem", color: "#ef4444", fontWeight: 700 }}>{s.pendingReferrals}</td>
                  <td style={{ padding: "0.6rem 0.5rem", color: "#3b82f6" }}>{s.acceptedReferrals}</td>
                  <td style={{ padding: "0.6rem 0.5rem" }}>
                    <span style={{
                      background: SEVERITY_COLOR[s.severity] + "20",
                      color: SEVERITY_COLOR[s.severity],
                      borderRadius: 999, padding: "0.2rem 0.6rem", fontSize: "0.75rem", fontWeight: 700,
                    }}>
                      {s.severity}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* ── Section 2: Speciality Imbalances ── */}
      <div style={{ background: "white", borderRadius: 12, padding: "1.5rem", border: "1px solid #fecaca", marginBottom: "1.5rem" }}>
        <SectionHeader title="Speciality Imbalances" count={imbalances.length} />
        {imbalances.length === 0 ? (
          <div style={{ color: "#9ca3af", fontSize: "0.9rem" }}>No imbalances detected.</div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
            {imbalances.map((sp, idx) => (
              <div key={`${sp.specialityCode}-${idx}`} style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
                <div style={{ flex: "0 0 150px", fontSize: "0.875rem", fontWeight: 600, color: "#374151" }}>
                  {sp.specialityName}
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.75rem", color: "#9ca3af", marginBottom: 4 }}>
                    <span>{sp.completedReferrals} / {sp.totalReferrals} completed</span>
                    <span>{sp.completionRate}%</span>
                  </div>
                  <div style={{ background: "#f3f4f6", borderRadius: 4, height: 8, overflow: "hidden" }}>
                    <div style={{
                      background: sp.completionRate < 30 ? "#ef4444" : sp.completionRate < 60 ? "#f97316" : "#22c55e",
                      height: "100%", width: `${sp.completionRate}%`, transition: "width 0.6s ease",
                    }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* ── Section 3: Cross-State Warnings ── */}
      <div style={{ background: "white", borderRadius: 12, padding: "1.5rem", border: "1px solid #fecaca" }}>
        <SectionHeader title="Cross-State Warnings" count={warnings.length} />
        {warnings.length === 0 ? (
          <div style={{ color: "#9ca3af", fontSize: "0.9rem" }}>No cross-state referrals detected.</div>
        ) : (
          <div style={{ display: "flex", flexDirection: "column", gap: "0.75rem" }}>
            {warnings.map((w) => (
              <div key={w.referralId} style={{
                background: "#fff7ed", border: "1px solid #fed7aa", borderRadius: 8, padding: "0.75rem 1rem",
                display: "flex", justifyContent: "space-between", alignItems: "center", gap: "1rem",
              }}>
                <div>
                  <div style={{ fontSize: "0.875rem", fontWeight: 600, color: "#374151" }}>
                    Patient: {w.patientName}
                    <span style={{ marginLeft: 8, fontSize: "0.75rem", color: "#9ca3af" }}>({w.patientState})</span>
                  </div>
                  <div style={{ fontSize: "0.8rem", color: "#6b7280", marginTop: 2 }}>
                    {w.fromDoctorName} → {w.toDoctorName}
                  </div>
                </div>
                <span style={{
                  background: "#fef3c7", color: "#92400e",
                  borderRadius: 999, padding: "0.2rem 0.6rem", fontSize: "0.75rem", fontWeight: 600, whiteSpace: "nowrap",
                }}>
                  Out-of-state
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
