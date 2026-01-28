"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { signOut } from "firebase/auth";
import { auth } from "@/lib/firebase";
import styles from "../../admin.module.scss";

type Props = {
  redirectTo?: string; // optional override for where to send the user after sign-out
};

export default function SignOutButton({ redirectTo = "/public/admin/login" }: Props) {
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  async function handleSignOut() {
    if (loading) return;
    setLoading(true);
    try {
      await signOut(auth);
      // Optional: clear any client-side state/caches here

      // Redirect to login (or provided path)
      router.push(redirectTo);
    } catch (err) {
      console.error("Sign out failed", err);
      // User-friendly fallback
      alert("Sign out failed. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <button
      type="button"
      onClick={handleSignOut}
      disabled={loading}
      aria-label="Sign out"
      className={styles.viewAppointmentsBtn}
    >
      {loading ? "Signing out…" : "Sign out"}
    </button>
  );
}