"use client";

import { useEffect, useState } from "react";
import { getFacilities } from "@/lib/admin/adminApi";
import { auth } from "@/lib/firebase";
import { onAuthStateChanged } from "firebase/auth";
import type { Facility } from "../../types";

export function useFacilities() {
  const [facilities, setFacilities] = useState<Facility[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function load() {
      // wait for Firebase auth to be ready and for a signed-in user
      const token = await new Promise<string | null>((resolve) => {
        const unsubscribe = onAuthStateChanged(auth, async (user) => {
          unsubscribe();
          if (!user) {
            resolve(null);
            return;
          }
          try {
            const t = await user.getIdToken(true);
            resolve(t);
          } catch (e) {
            console.warn("useFacilities: failed to get id token", e);
            resolve(null);
          }
        });
        // safety: if onAuthStateChanged doesn't fire within 3s resolve null
        setTimeout(() => {
          try { unsubscribe(); } catch {}
          resolve(null);
        }, 3000);
      });

      if (!token) {
        setLoading(false);
        return;
      }

      try {
        const data = await getFacilities(token);
        setFacilities(data);
      } catch (err) {
        console.error("Failed to load facilities:", err);
        setFacilities([]);
      } finally {
        setLoading(false);
      }
    }

    load();
    // listen for external signals to refresh facilities (e.g. after create/update)
    const handler = () => {
      // call reload to refresh list
      reload().catch((e) => console.warn('reload failed from event', e));
    };
    try {
      window.addEventListener('wecureit:facilities-changed', handler as EventListener);
    } catch {}

    return () => {
      try {
        window.removeEventListener('wecureit:facilities-changed', handler as EventListener);
      } catch {}
    };
  }, []);

  async function reload() {
    setLoading(true);
    // force refresh the token to ensure any recent role/claim changes are present
    const token = await auth.currentUser?.getIdToken(true);
    if (!token) {
      setFacilities([]);
      setLoading(false);
      return;
    }

    try {
      const data = await getFacilities(token);
      setFacilities(data);
    } catch {
      setFacilities([]);
    } finally {
      setLoading(false);
    }
  }

  function replaceFacility(updated: Facility) {
    setFacilities((prev) => prev.map((f) => (f.id === updated.id ? updated : f)));
  }

  return { facilities, loading, reload, replaceFacility };
}

