"use client";

import { useEffect, useState } from "react";
import { getFacilities } from "@/lib/admin/adminApi";
import { auth } from "@/lib/firebase";
import type { Facility } from "../../types";

export function useFacilities() {
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function load() {
      const token = await auth.currentUser?.getIdToken();
      if (!token) {
        setLoading(false);
        return;
      }

      // use promise chaining instead of try/catch/finally so we avoid those constructs
      getFacilities(token)
        .then((data) => setFacilities(data))
        .catch(() => setFacilities([]))
        .then(() => setLoading(false));
    }

    load();
  }, []);

  return { facilities, loading };
}

