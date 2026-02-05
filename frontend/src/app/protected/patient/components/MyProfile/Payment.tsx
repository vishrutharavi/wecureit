"use client";

import React, { useState, useEffect } from "react";
import styles from "../../patient.module.scss";
import { FiCreditCard } from "react-icons/fi";
import { apiFetch, showInlineToast } from "@/lib/api";
import { auth } from '@/lib/firebase';

// Inline SectionCard and ReadField so this file is self-contained (Card.tsx may be removed)
export function ReadField({ children }: { children: React.ReactNode }) {
  return <div className={styles.readField}>{children}</div>;
}

type SectionCardProps = {
  icon?: React.ReactNode;
  title: string;
  subtitle?: string;
  children?: React.ReactNode;
};

function SectionCard({ icon, title, subtitle, children }: SectionCardProps) {
  return (
    <div className={`${styles.panelWhite} ${styles.profilePanel} ${styles.sectionCard}`}>
      <div className={styles.sectionHeader}>
        <div className={styles.sectionIcon}>{icon}</div>
        <div>
          <div className={styles.sectionTitle}>{title}</div>
          {subtitle ? <div className={styles.sectionSubtitle}>{subtitle}</div> : null}
        </div>
      </div>

      <div className={styles.cardContent}>{children}</div>
    </div>
  );
}

function maskLast4(last4?: string) {
  if (!last4) return "No card on file";
  const digits = String(last4).replace(/\D/g, "");
  if (digits.length === 4) return `**** **** **** ${digits}`;
  return `**** **** **** ${digits.slice(-4)}`;
}

type SavedCard = { id: string; last4: string; expiry?: string };

export default function Payment() {
  const [adding, setAdding] = useState(false);
  const [cards, setCards] = useState<SavedCard[]>([]);
  const [patientId, setPatientId] = useState<string | null>(null);

  // card form
  const [newCardNumber, setNewCardNumber] = useState("");
  const [newExpiry, setNewExpiry] = useState("");
  const [newCvv, setNewCvv] = useState("");
  const [errors, setErrors] = useState<{ card?: string; expiry?: string; cvv?: string }>({});

  const currentYear = new Date().getFullYear();

  async function getFreshToken(): Promise<string | undefined> {
    try {
      if (typeof window !== 'undefined' && auth && auth.currentUser) {
        const t = await auth.currentUser.getIdToken(true);
        try { localStorage.setItem('patientToken', t); } catch {}
        return t;
      }
    } catch (e) {
      // ignore and fall back to stored token
    }
    try { return localStorage.getItem('patientToken') ?? undefined; } catch { return undefined; }
  }

  useEffect(() => {
    // initialize: ensure we have patientId (from localStorage.patientProfile or /api/patient/me) then fetch cards
    async function init() {
      try {
        const raw = typeof window !== "undefined" ? localStorage.getItem("patientProfile") : null;
        let profile = raw ? JSON.parse(raw) : {};
        let id: string | undefined = profile?.id;
        if (!id) {
          // call backend to resolve patient DB id (apiFetch will attach token)
          try {
            const token = await getFreshToken();
            const me = await apiFetch("/api/patient/me", token);
            if (me && me.id) {
              id = String(me.id);
              profile = { ...profile, id, name: profile.name ?? me.name ?? profile.name };
              try { localStorage.setItem('patientProfile', JSON.stringify(profile)); } catch {}
            }
          } catch (e) {
            // ignore - we may not be able to call backend, fall back to local only state
          }
        }
        if (id) {
          setPatientId(id);
          await fetchCards(id);
        } else {
          // load any locally-stored cards as a fallback
          try {
            const p = profile || {};
            const localCards = p?.payment?.cards || [];
            setCards(Array.isArray(localCards) ? localCards : []);
          } catch {}
        }
      } catch {}
    }
    init();
  }, []);

  async function fetchCards(id: string) {
    try {
      console.debug('[Payment] fetchCards: fetching cards for patientId=', id);
      // obtain a fresh token if possible to avoid sending an expired token
      const token = await getFreshToken();
      const res = await apiFetch(`/cards/getcards?patientId=${id}`, token);
      if (Array.isArray(res)) {
        const next: SavedCard[] = res.map((r: any) => ({ id: String(r.id), last4: r.last4 }));
        setCards(next);
        // persist a lightweight view for other client components
        try {
          const raw = localStorage.getItem('patientProfile');
          const p = raw ? JSON.parse(raw) : {};
          p.payment = p.payment || {};
          p.payment.cards = next;
          localStorage.setItem('patientProfile', JSON.stringify(p));
          try { window.dispatchEvent(new CustomEvent('patient:cardsUpdated')); } catch {}
        } catch {}
      }
    } catch (err) {
      console.error('[Payment] fetchCards: failed to fetch cards for patientId=', id, err);
      try { showInlineToast('Failed to load saved cards'); } catch {}
      // leave cards as-is; apiFetch may have shown a friendlier error
    }
  }

  function startAdd() {
    setAdding(true);
    setNewCardNumber("");
    setNewExpiry("");
    setNewCvv("");
  }

  function handleCardNumberChange(raw: string) {
    const digits = raw.replace(/\D/g, "").slice(0, 16);
    setNewCardNumber(digits);
  }

  function handleCvvChange(raw: string) {
    const digits = raw.replace(/\D/g, "").slice(0, 3);
    setNewCvv(digits);
  }

  function formatCardNumberDisplay(digits: string) {
    return digits.replace(/(\d{4})/g, "$1 ").trim();
  }

  function validateNewCard() {
    const nextErrors: { card?: string; expiry?: string; cvv?: string } = {};
    if (newCardNumber.length !== 16) nextErrors.card = "Card number must be 16 digits";
    // Expect MM/YYYY format
    const m = (newExpiry || "").trim();
    const match = m.match(/^\s*(\d{1,2})\s*\/\s*(\d{2,4})\s*$/);
    if (!match) {
      nextErrors.expiry = "Expiry must be in MM/YYYY format";
    } else {
      const monthNum = parseInt(match[1], 10);
      let yearNum = parseInt(match[2], 10);
      if (match[2].length === 2) yearNum = 2000 + yearNum;
      if (monthNum < 1 || monthNum > 12) nextErrors.expiry = "Month must be 1-12";
      if (yearNum < currentYear || yearNum > 2099) nextErrors.expiry = `Year must be between ${currentYear} and 2099`;
      if (!nextErrors.expiry && yearNum === currentYear) {
        const thisMonth = new Date().getMonth() + 1;
        if (monthNum < thisMonth) nextErrors.expiry = "Expiry cannot be in the past";
      }
    }
    if (newCvv.length !== 3) nextErrors.cvv = "CVV must be 3 digits";
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function saveNewCard() {
    if (!validateNewCard()) return;
    if (!patientId) {
      // try to resolve patient id one more time
      try {
        const token = await getFreshToken();
        const me = await apiFetch('/api/patient/me', token);
        if (me && me.id) {
          setPatientId(String(me.id));
        } else {
          throw new Error('Unable to resolve patient id');
        }
      } catch (e) {
        // apiFetch will show friendly message if auth fails
        return;
      }
    }

    // normalize expiry
    const norm = (newExpiry || '').match(/^(\d{1,2})\/?(\d{2,4})$/);
    let mm = 0;
    let yyyy = 0;
    if (norm) {
      mm = parseInt(norm[1], 10);
      yyyy = parseInt(norm[2], 10);
      if (String(norm[2]).length === 2) yyyy = 2000 + yyyy;
    }

    try {
      const body = {
        pan: newCardNumber,
        cvc: newCvv,
        expMonth: mm,
        expYear: yyyy,
        patientMasterId: patientId,
      } as any;

  const token = await getFreshToken();
  const created = await apiFetch('/cards/add', token, { method: 'POST', body: JSON.stringify(body) });
      // created should be { id, last4 }
      if (created && created.id) {
        const added: SavedCard = { id: String(created.id), last4: String(created.last4), expiry: `${String(mm).padStart(2,'0')}/${String(yyyy)}` };
        const next = [...cards, added];
        setCards(next);
        try {
          const raw = localStorage.getItem('patientProfile');
          const p = raw ? JSON.parse(raw) : {};
          p.payment = p.payment || {};
          p.payment.cards = next;
          localStorage.setItem('patientProfile', JSON.stringify(p));
          try { window.dispatchEvent(new CustomEvent('patient:cardsUpdated')); } catch {}
        } catch {}
      }
    } catch (err) {
      // apiFetch already shows errors via toast; do nothing here
    }

    setAdding(false);
    setNewCardNumber("");
    setNewExpiry("");
    setNewCvv("");
  }

  async function deleteCard(id: string) {
    if (!patientId) return;
    try {
      const token = await getFreshToken();
      await apiFetch(`/cards/${id}?patientId=${patientId}`, token, { method: 'DELETE' });
      const next = cards.filter((c) => c.id !== id);
      setCards(next);
      try {
        const raw = localStorage.getItem('patientProfile');
        const p = raw ? JSON.parse(raw) : {};
        p.payment = p.payment || {};
        p.payment.cards = next;
        localStorage.setItem('patientProfile', JSON.stringify(p));
        try { window.dispatchEvent(new CustomEvent('patient:cardsUpdated')); } catch {}
      } catch {}
    } catch (err) {
      // apiFetch will surface friendly errors
    }
  }

  function cancelAdd() {
    setAdding(false);
    setNewCardNumber("");
    setNewExpiry("");
    setNewCvv("");
  }

  return (
    <SectionCard icon={<FiCreditCard size={18} />} title="Payment Information" subtitle="Your saved card details">
      <div className={styles.sectionGrid}>
        <div className={styles.sectionActionsRight}>
          {!adding ? (
            <button className={styles.editProfileBtn} onClick={startAdd}>
              ✎ Add Card
            </button>
          ) : (
            <div className={styles.actionRow}>
              <button onClick={cancelAdd} className={styles.cancelSecondary}>
                ✕ Cancel
              </button>
              <button className={styles.viewAppointmentsBtn} onClick={saveNewCard}>
                Save Card
              </button>
            </div>
          )}
        </div>

        {adding ? (
          <div>
            <div className={"fieldLabel"}>Card Number</div>
            <input
              className={styles.inputField}
              value={formatCardNumberDisplay(newCardNumber)}
              onChange={(e) => handleCardNumberChange(e.target.value)}
              placeholder="1234 5678 9012 3456"
              inputMode="numeric"
              pattern="\d*"
              maxLength={19} /* spaced format length */
            />
            {errors.card ? <div className={styles.errorText}>{errors.card}</div> : null}

            <div className={`${styles.paymentGrid} ${styles.paymentGridSpacing}`}>
              <div>
                <div className={"fieldLabel"}>Expiry Date</div>
                <div style={{ display: "flex", gap: 8 }}>
                  <input
                    className={`${styles.inputField}`}
                    placeholder="MM/YYYY"
                    value={newExpiry}
                    inputMode="numeric"
                    pattern="\d{2}/\d{2,4}"
                    onChange={(e) => {
                      let v = e.target.value.replace(/[^0-9\/]/g, "");
                      const digitsOnly = v.replace(/\D/g, '');
                      if (/^\d{3,4}$/.test(digitsOnly) && !v.includes('/')) {
                        v = digitsOnly.slice(0,2) + '/' + digitsOnly.slice(2);
                      }
                      setNewExpiry(v.slice(0,7));
                    }}
                  />
                </div>
                {errors.expiry ? <div className={styles.errorText}>{errors.expiry}</div> : null}
              </div>

                <div>
                  <div className={"fieldLabel"}>CVV</div>
                  <input className={styles.inputField} value={newCvv} onChange={(e) => handleCvvChange(e.target.value)} inputMode="numeric" maxLength={3} />
                  {errors.cvv ? <div className={styles.errorText}>{errors.cvv}</div> : null}
                </div>
            </div>
          </div>
        ) : (
          <>
            {cards.length === 0 ? (
              <div>
                <div className={"fieldLabel"}>Card Number</div>
                <ReadField>No card on file</ReadField>
              </div>
            ) : (
              cards.map((c) => (
                <div key={c.id} className={styles.cardListItem}>
                  <div className={styles.cardInnerRow}>
                    <div style={{ flex: 1 }}>
                      <div className={"fieldLabel"}>Card Number</div>
                      <ReadField>{maskLast4(c.last4)}</ReadField>
                      <div className={styles.paymentGrid} style={{ marginTop: 8 }}>
                        <div>
                          <div className={"fieldLabel"}>Expiry Date</div>
                          <ReadField>{c.expiry || "**/**"}</ReadField>
                        </div>

                        <div>
                          <div className={"fieldLabel"}>CVV</div>
                          <ReadField>***</ReadField>
                        </div>
                      </div>
                    </div>

                    <div>
                      <button onClick={() => deleteCard(c.id)} className={styles.deleteSecondary}>
                        Delete
                      </button>
                    </div>
                  </div>
                </div>
              ))
            )}
          </>
        )}
      </div>
    </SectionCard>
  );
}
