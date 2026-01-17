// components/Facilities/useFacilities.ts
import { useEffect, useState } from "react";
import { Facility } from "../../types";
import { apiFetch } from "@/lib/api";

export function useFacilities(token?: string) {
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchFacilities() {
      try {
        setLoading(true);

        const data = await apiFetch(
          "/api/admin/facilities",
          token
        );

        setFacilities(data);
      } catch (err: any) {
        setError(err.message ?? "Failed to load facilities");
      } finally {
        setLoading(false);
      }
    }

    fetchFacilities();
  }, [token]);

  return {
    facilities,
    loading,
    error,
    setFacilities,
  };
}
