"use client";

import React, { useEffect, useState, useCallback } from "react";
import {
  LineChart, Line, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell,
} from "recharts";
import { auth } from "@/lib/firebase";
import {
  getReferralOverview,
  getReferralTrends,
  getReferralsByDoctor,
  getReferralsBySpeciality,
} from "@/lib/admin/adminApi";

type Overview = {
  totalReferrals: number;
  pendingCount: number;
  acceptedCount: number;
  completedCount: number;
  cancelledCount: number;
  completionRate: number;
  avgResponseTimeHours: number;
  topSpeciality: string;
};
type TrendPoint = { date: string; count: number };
type DoctorStat  = { doctorId: string; doctorName: string; outgoingCount: number; incomingCount: number };
type SpecStat    = { specialityCode: string; specialityName: string; totalCount: number; completedCount: number; completionRate: number };

const REDS = ["#ef4444", "#f97316", "#f43f5e", "#dc2626", "#fb923c", "#e11d48", "#b91c1c", "#ea580c"];

function StatCard({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div style={{
      background: "white", borderRadius: 12, padding: "1.25rem 1.5rem",
      border: "1px solid #fecaca", boxShadow: "0 4px 16px rgba(239,68,68,0.08)",
      flex: "1 1 180px", minWidth: 160,
    }}>
      <div style={{ fontSize: "0.8rem", color: "#9ca3af", fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.05em" }}>{label}</div>
      <div style={{ fontSize: "2rem", fontWeight: 800, color: "#ef4444", marginTop: 4 }}>{value}</div>
      {sub && <div style={{ fontSize: "0.8rem", color: "#6b7280", marginTop: 2 }}>{sub}</div>}
    </div>
  );
}

export default function OverviewDashboard() {
  const [overview, setOverview] = useState<Overview | null>(null);
  const [trends, setTrends]     = useState<TrendPoint[]>([]);
  const [doctors, setDoctors]   = useState<DoctorStat[]>([]);
  const [specs, setSpecs]       = useState<SpecStat[]>([]);
  const [loading, setLoading]   = useState(true);

  const fetchAll = useCallback(async () => {
    const user = auth.currentUser;
    if (!user) return;
    try {
      setLoading(true);
      const token = await user.getIdToken();
      const [ov, tr, doc, sp] = await Promise.all([
        getReferralOverview(token),
        getReferralTrends(token, 30),
        getReferralsByDoctor(token),
        getReferralsBySpeciality(token),
      ]);
      if (ov)  setOverview(ov as Overview);
      if (Array.isArray(tr))  setTrends(tr as TrendPoint[]);
      if (Array.isArray(doc)) setDoctors(doc as DoctorStat[]);
      if (Array.isArray(sp))  setSpecs(sp as SpecStat[]);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchAll(); }, [fetchAll]);

  // Compact date labels for the x-axis (e.g. "Feb 14")
  const trendData = trends.map((t) => ({
    ...t,
    label: new Date(t.date + "T00:00:00").toLocaleDateString("en-US", { month: "short", day: "numeric" }),
  }));

  if (loading) {
    return (
      <div style={{ textAlign: "center", padding: "4rem", color: "#9ca3af" }}>
        Loading analytics…
      </div>
    );
  }

  return (
    <div style={{ marginTop: "2rem" }}>
      {/* ── Stat Cards ── */}
      <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", marginBottom: "2rem" }}>
        <StatCard label="Total Referrals"   value={overview?.totalReferrals ?? 0} />
        <StatCard label="Pending"           value={overview?.pendingCount ?? 0}   sub="awaiting action" />
        <StatCard label="Completed"         value={overview?.completedCount ?? 0} sub={`${overview?.completionRate ?? 0}% rate`} />
        <StatCard label="Avg Wait Time"     value={`${overview?.avgResponseTimeHours ?? 0}h`} sub="to accept" />
        <StatCard label="Top Speciality"    value={overview?.topSpeciality ?? "—"} />
      </div>

      {/* ── Charts row ── */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1.5rem", marginBottom: "2rem" }}>
        {/* Trend line chart */}
        <div style={{ background: "white", borderRadius: 12, padding: "1.5rem", border: "1px solid #fecaca" }}>
          <h3 style={{ margin: "0 0 1rem", fontSize: "1rem", fontWeight: 700, color: "#7f1d1d" }}>
            Referrals — Last 30 Days
          </h3>
          <ResponsiveContainer width="100%" height={220}>
            <LineChart data={trendData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#fee2e2" />
              <XAxis dataKey="label" tick={{ fontSize: 11, fill: "#9ca3af" }} interval={6} />
              <YAxis allowDecimals={false} tick={{ fontSize: 11, fill: "#9ca3af" }} />
              <Tooltip
                contentStyle={{ borderRadius: 8, border: "1px solid #fecaca", fontSize: 12 }}
                formatter={(v: number | undefined) => [v ?? 0, "Referrals"]}
              />
              <Line type="monotone" dataKey="count" stroke="#ef4444" strokeWidth={2} dot={false} activeDot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Speciality bar chart */}
        <div style={{ background: "white", borderRadius: 12, padding: "1.5rem", border: "1px solid #fecaca" }}>
          <h3 style={{ margin: "0 0 1rem", fontSize: "1rem", fontWeight: 700, color: "#7f1d1d" }}>
            By Speciality
          </h3>
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={specs.slice(0, 8)} layout="vertical">
              <CartesianGrid strokeDasharray="3 3" stroke="#fee2e2" horizontal={false} />
              <XAxis type="number" allowDecimals={false} tick={{ fontSize: 11, fill: "#9ca3af" }} />
              <YAxis type="category" dataKey="specialityName" width={90} tick={{ fontSize: 11, fill: "#374151" }} />
              <Tooltip
                contentStyle={{ borderRadius: 8, border: "1px solid #fecaca", fontSize: 12 }}
                formatter={(v: number | undefined) => [v ?? 0, "Referrals"]}
              />
              <Bar dataKey="totalCount" radius={[0, 4, 4, 0]}>
                {specs.slice(0, 8).map((_, i) => (
                  <Cell key={i} fill={REDS[i % REDS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* ── Status breakdown ── */}
      <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1.5rem" }}>
        {/* Status distribution */}
        <div style={{ background: "white", borderRadius: 12, padding: "1.5rem", border: "1px solid #fecaca" }}>
          <h3 style={{ margin: "0 0 1rem", fontSize: "1rem", fontWeight: 700, color: "#7f1d1d" }}>
            Status Distribution
          </h3>
          {overview && (() => {
            const total = overview.totalReferrals || 1;
            const statuses = [
              { label: "Pending",   count: overview.pendingCount,   color: "#f97316" },
              { label: "Accepted",  count: overview.acceptedCount,  color: "#3b82f6" },
              { label: "Completed", count: overview.completedCount, color: "#22c55e" },
              { label: "Cancelled", count: overview.cancelledCount, color: "#9ca3af" },
            ];
            return statuses.map(({ label, count, color }) => (
              <div key={label} style={{ marginBottom: "0.75rem" }}>
                <div style={{ display: "flex", justifyContent: "space-between", fontSize: "0.85rem", marginBottom: 4 }}>
                  <span style={{ color: "#374151", fontWeight: 500 }}>{label}</span>
                  <span style={{ color: "#6b7280" }}>{count} ({Math.round(count / total * 100)}%)</span>
                </div>
                <div style={{ background: "#f3f4f6", borderRadius: 4, height: 8, overflow: "hidden" }}>
                  <div style={{ background: color, height: "100%", width: `${Math.round(count / total * 100)}%`, transition: "width 0.6s ease" }} />
                </div>
              </div>
            ));
          })()}
        </div>

        {/* Top doctors table */}
        <div style={{ background: "white", borderRadius: 12, padding: "1.5rem", border: "1px solid #fecaca" }}>
          <h3 style={{ margin: "0 0 1rem", fontSize: "1rem", fontWeight: 700, color: "#7f1d1d" }}>
            Top Doctors by Activity
          </h3>
          <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "0.85rem" }}>
            <thead>
              <tr style={{ color: "#9ca3af", textAlign: "left" }}>
                <th style={{ paddingBottom: "0.5rem", fontWeight: 600 }}>Doctor</th>
                <th style={{ paddingBottom: "0.5rem", fontWeight: 600 }}>Out</th>
                <th style={{ paddingBottom: "0.5rem", fontWeight: 600 }}>In</th>
              </tr>
            </thead>
            <tbody>
              {doctors.map((d, i) => (
                <tr key={d.doctorId} style={{ borderTop: i > 0 ? "1px solid #f3f4f6" : undefined }}>
                  <td style={{ padding: "0.4rem 0", color: "#374151" }}>{d.doctorName}</td>
                  <td style={{ padding: "0.4rem 0", color: "#ef4444", fontWeight: 700 }}>{d.outgoingCount}</td>
                  <td style={{ padding: "0.4rem 0", color: "#3b82f6", fontWeight: 700 }}>{d.incomingCount}</td>
                </tr>
              ))}
              {doctors.length === 0 && (
                <tr><td colSpan={3} style={{ color: "#9ca3af", paddingTop: "1rem" }}>No data yet</td></tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
