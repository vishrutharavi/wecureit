"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import dynamic from "next/dynamic";
import styles from "./admin.module.scss";
import AdminHeader from "./components/AdminHeader";
import AdminTabs, { type AdminTab, type AnalyticsSubTab } from "./components/AdminTabs";
import DoctorGrid from "./components/Doctors/DoctorGrid";
import FacilityGrid from "./components/Facilities/FacilityGrid";
import AddDoctorModal from "./components/Doctors/AddDoctorModal";
import AddFacilityModal from "./components/Facilities/AddFacilityModal";
import type { Doctor } from "./types";
import { useDoctors } from "./components/Doctors/useDoctors";
import { useDoctorMeta } from "./components/Doctors/useDoctors";
import { auth } from "@/lib/firebase";
import { deactivateDoctor } from "@/lib/admin/adminApi";

const OverviewDashboard = dynamic(() => import("./components/Analytics/OverviewDashboard"), { ssr: false });
const AlertsDashboard   = dynamic(() => import("./components/Analytics/AlertsDashboard"),   { ssr: false });
const NetworkGraph      = dynamic(() => import("./components/Analytics/NetworkGraph"),       { ssr: false });

export default function AdminPage() {
  const [tab, setTab] = useState<AdminTab>("doctors");
  const [analyticsSubTab, setAnalyticsSubTab] = useState<AnalyticsSubTab>("overview");

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      const params = new URLSearchParams(window.location.search);
      const t = params.get("tab") as AdminTab | null;
      if (t && (["doctors", "facilities", "analytics"] as AdminTab[]).includes(t)) {
        setTimeout(() => setTab(t), 0);
      }
      const s = params.get("sub") as AnalyticsSubTab | null;
      if (s && (["overview", "alerts", "network"] as AnalyticsSubTab[]).includes(s)) {
        setTimeout(() => setAnalyticsSubTab(s), 0);
      }
    } catch {}
  }, []);

  const router = useRouter();

  useEffect(() => {
    let unsub = () => {};
    try {
      unsub = auth.onAuthStateChanged((user) => {
        if (!user) { try { router.push("/public/admin/login"); } catch {} }
      });
    } catch {}
    return () => { try { unsub(); } catch {} };
  }, [router]);

  // Sync tab + sub-tab to URL
  useEffect(() => {
    try {
      const params = new URLSearchParams(window.location.search);
      params.set("tab", tab);
      if (tab === "analytics") params.set("sub", analyticsSubTab);
      else params.delete("sub");
      window.history.replaceState({}, "", `${window.location.pathname}?${params.toString()}`);
    } catch {}
  }, [tab, analyticsSubTab]);

  const [showAdd, setShowAdd] = useState(false);
  const [editingDoctor, setEditingDoctor] = useState<Doctor | null>(null);

  const { doctors: fetchedDoctors, loading: doctorsLoading } = useDoctors();
  const { states: metaStates, specialities: metaSpecialities } = useDoctorMeta();

  const [localAddedDoctors, setLocalAddedDoctors] = useState<Doctor[]>([]);
  const [doctorOverrides, setDoctorOverrides] = useState<Record<string, Doctor>>({});
  const [deletedDoctorIds, setDeletedDoctorIds] = useState<Set<string>>(new Set());

  const doctors = [
    ...localAddedDoctors,
    ...(fetchedDoctors ?? []).map((d) => doctorOverrides[d.id] ?? d),
  ];
  const sortedVisibleDoctors = [...doctors.filter((d) => !deletedDoctorIds.has(d.id))].sort(
    (a, b) => (a.name || "").localeCompare(b.name || "", undefined, { sensitivity: "base" })
  );

  async function handleDelete(doctor: Doctor) {
    const user = auth.currentUser;
    if (!user) { alert("Not authenticated"); return; }
    if (!confirm(`Delete doctor ${doctor.name ?? doctor.email}?`)) return;
    try {
      const token = await user.getIdToken();
      await deactivateDoctor(token, doctor.id);
      setDeletedDoctorIds((prev) => new Set(prev).add(doctor.id));
      setLocalAddedDoctors((prev) => prev.filter((d) => d.id !== doctor.id));
      setDoctorOverrides((prev) => { const c = { ...prev }; delete c[doctor.id]; return c; });
    } catch (err: unknown) {
      alert((err as { message?: string })?.message ?? "Failed to delete doctor");
    }
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.content}>
        <AdminHeader />
        <AdminTabs
          active={tab}
          onChange={setTab}
          analyticsSubTab={analyticsSubTab}
          onAnalyticsSubTabChange={setAnalyticsSubTab}
        />

        {tab === "doctors" || tab === "facilities" ? (
          <div className={styles.card}>
            <div className={styles.cardHeader}>
              <div>
                <h2>{tab === "doctors" ? "Doctor Management" : "Facility Management"}</h2>
                <p>{tab === "doctors" ? "Create and manage doctors" : "Add and manage medical facilities with rooms"}</p>
              </div>
              <button
                className={styles.viewAppointmentsBtn}
                onClick={() => { setEditingDoctor(null); setShowAdd(true); }}
              >
                {tab === "doctors" ? "Add Doctor" : "Add Facility"}
              </button>
            </div>

            {tab === "doctors" ? (
              !doctorsLoading ? (
                <DoctorGrid
                  doctors={sortedVisibleDoctors}
                  metaStates={metaStates}
                  metaSpecialities={metaSpecialities}
                  onEdit={(d) => { setEditingDoctor(d); setShowAdd(true); }}
                  onDelete={handleDelete}
                />
              ) : (
                <div>Loading doctors...</div>
              )
            ) : (
              <FacilityGrid />
            )}
          </div>
        ) : analyticsSubTab === "overview" ? (
          <OverviewDashboard />
        ) : analyticsSubTab === "alerts" ? (
          <AlertsDashboard />
        ) : (
          <NetworkGraph />
        )}
      </div>

      {showAdd && (tab === "doctors" || tab === "facilities") && (
        tab === "doctors" ? (
          <AddDoctorModal
            initialDoctor={editingDoctor ?? undefined}
            onCreated={(d) => setLocalAddedDoctors((prev: Doctor[]) => [d, ...prev])}
            onUpdated={(d: Doctor) => setDoctorOverrides((prev) => ({ ...prev, [d.id]: d }))}
            onClose={() => setShowAdd(false)}
          />
        ) : (
          <AddFacilityModal onClose={() => setShowAdd(false)} />
        )
      )}
    </div>
  );
}
