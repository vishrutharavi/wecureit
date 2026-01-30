"use client";

import React, { useState, useEffect } from "react";
import styles from "../../patient.module.scss";

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
import { FiCreditCard } from "react-icons/fi";

function maskCard(card: string) {
  if (!card) return "No card on file";
  const digits = card.replace(/\D/g, "");
  if (digits.length <= 4) return digits;
  const last4 = digits.slice(-4);
  return `**** **** **** ${last4}`;
}

export default function Payment() {
  // store multiple cards as patientProfile.payment.cards = [{id, cardNumber, expiry, cvv}]
  const [adding, setAdding] = useState(false);
  // start empty on first render (server + initial client render must match)
  const [cards, setCards] = useState<Array<{ id: string; cardNumber: string; expiry: string; cvv: string }>>([]);

  // load persisted cards after mount to avoid SSR/CSR markup mismatch
  useEffect(() => {
    try {
      const raw = localStorage.getItem("patientProfile");
      if (raw) {
        const p = JSON.parse(raw);
        // schedule state update to avoid synchronous setState in effect
        const next = p?.payment?.cards || [];
        setTimeout(() => setCards(next), 0);
      }
    } catch {}
  }, []);

  const [newCardNumber, setNewCardNumber] = useState("");
  const [newExpiry, setNewExpiry] = useState("");
  const [newCvv, setNewCvv] = useState("");
  const [errors, setErrors] = useState<{ card?: string; expiry?: string; cvv?: string }>({});

  const currentYear = new Date().getFullYear();

  function startAdd() {
    setAdding(true);
    setNewCardNumber("");
    setNewExpiry("");
    setNewCvv("");
  }

  function formatCardNumberDisplay(digits: string) {
    return digits.replace(/(\d{4})/g, "$1 ").trim();
  }

  function handleCardNumberChange(raw: string) {
    // keep only digits, max 16
    const digits = raw.replace(/\D/g, "").slice(0, 16);
    setNewCardNumber(digits);
  }

  function handleCvvChange(raw: string) {
    const digits = raw.replace(/\D/g, "").slice(0, 3);
    setNewCvv(digits);
  }

  function validateNewCard() {
    const nextErrors: { card?: string; expiry?: string; cvv?: string } = {};
    if (newCardNumber.length !== 16) nextErrors.card = "Card number must be 16 digits";
    // Expect MM/YYYY format
    const m = (newExpiry || "").trim();
    const match = m.match(/^\s*(\d{1,2})\s*\/\s*(\d{4})\s*$/);
    if (!match) {
      nextErrors.expiry = "Expiry must be in MM/YYYY format";
    } else {
      const monthNum = parseInt(match[1], 10);
      const yearNum = parseInt(match[2], 10);
        if (monthNum < 1 || monthNum > 12) nextErrors.expiry = "Month must be 1-12";
        // allow current year up to 2099
        if (yearNum < currentYear || yearNum > 2099) nextErrors.expiry = `Year must be between ${currentYear} and 2099`;
      // if year equals currentYear, ensure month not in past
      if (!nextErrors.expiry && yearNum === currentYear) {
        const thisMonth = new Date().getMonth() + 1;
        if (monthNum < thisMonth) nextErrors.expiry = "Expiry cannot be in the past";
      }
    }
    if (newCvv.length !== 3) nextErrors.cvv = "CVV must be 3 digits";
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  function saveNewCard() {
    if (!validateNewCard()) return;
    const id = String(Date.now());
    // normalize expiry to MM/YYYY
    let expiry = newExpiry.trim();
    const norm = expiry.match(/^(\d{1,2})\/?(\d{2,4})$/);
    if (norm) {
      const mm = String(norm[1]).padStart(2, "0");
      let yyyy = norm[2];
      if (yyyy.length === 2) {
        // assume 20xx for two-digit years
        yyyy = '20' + yyyy;
      }
      expiry = `${mm}/${yyyy}`;
    }
    const next = [...cards, { id, cardNumber: newCardNumber, expiry, cvv: newCvv }];
    setCards(next);
    try {
      const raw = localStorage.getItem("patientProfile");
      const p = raw ? JSON.parse(raw) : {};
      p.payment = p.payment || {};
      p.payment.cards = next;
      localStorage.setItem("patientProfile", JSON.stringify(p));
      // notify other components in this window that cards changed
      try { window.dispatchEvent(new CustomEvent('patient:cardsUpdated')); } catch {};
    } catch {}
    setAdding(false);
  }

  function cancelAdd() {
    setAdding(false);
    setNewCardNumber("");
    setNewExpiry("");
    setNewCvv("");
  }

  function deleteCard(id: string) {
    const next = cards.filter((c) => c.id !== id);
    setCards(next);
    try {
      const raw = localStorage.getItem("patientProfile");
      const p = raw ? JSON.parse(raw) : {};
      p.payment = p.payment || {};
      p.payment.cards = next;
      localStorage.setItem("patientProfile", JSON.stringify(p));
      try { window.dispatchEvent(new CustomEvent('patient:cardsUpdated')); } catch {};
    } catch {}
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
              pattern="\\d*"
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
                    pattern="\\d{2}/\\d{2,4}"
                    onChange={(e) => {
                      // allow digits and slash, auto-insert slash when appropriate
                      let v = e.target.value.replace(/[^0-9\/]/g, "");
                      // if user types 4 digits like MMYY, insert slash after 2
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
                      <ReadField>{maskCard(c.cardNumber)}</ReadField>
                      <div className={styles.paymentGrid} style={{ marginTop: 8 }}>
                        <div>
                          <div className={"fieldLabel"}>Expiry Date</div>
                          <ReadField>{c.expiry || "**/**"}</ReadField>
                        </div>

                        <div>
                          <div className={"fieldLabel"}>CVV</div>
                          <ReadField>{c.cvv ? c.cvv.replace(/./g, "*") : "***"}</ReadField>
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
