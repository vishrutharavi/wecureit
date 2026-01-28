"use client";

import { useFacilities } from "./useFacilities";
import FacilityCard from "./FacilityCard";

export default function FacilityGrid() {
  const { facilities, loading, replaceFacility } = useFacilities();

  if (loading) return <p>Loading facilities...</p>;

  if (!facilities.length) {
    return <p style={{ padding: "1.5rem" }}>No facilities created yet.</p>;
  }

  return (
    <div style={{
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
      gap: "1.5rem",
      alignItems: "start",
      padding: "1.5rem"
    }}>
      {facilities.map(f => (
        <FacilityCard key={f.id} facility={f} onUpdated={(updated) => { if (updated) replaceFacility(updated); }} />
      ))}
    </div>
  );
}
