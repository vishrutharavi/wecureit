"use client";

import React, { useState } from "react";
import styles from "../../doctor.module.scss";

type Props = {
  open: boolean;
  onClose: () => void;
};

export default function MessageModal({ open, onClose }: Props) {
  const [viewIndex, setViewIndex] = useState<number | null>(null);
  const [replyIndex, setReplyIndex] = useState<number | null>(null);
  const [replyText, setReplyText] = useState<Record<number, string>>({});
  const [newTo, setNewTo] = useState("");
  const [newSubject, setNewSubject] = useState("");
  const [newBody, setNewBody] = useState("");

  if (!open) return null;

  const sample = [
    {
      from: "Dr. A. Patel",
      subject: "Consultation request",
      preview: "Can you review patient X's labs?",
      body: "Hi, could you please review the attached labs for patient X and advise if further imaging is needed? Thanks.",
    },
    {
      from: "Cardiology Dept.",
      subject: "Equipment booking",
      preview: "Please confirm the echo slot for 2/2.",
      body: "We have a free echo slot on 2/2 at 10:00 AM; please confirm if you want to reserve it for your patient.",
    },
    {
      from: "Patient: Maria Gomez",
      subject: "Follow-up question",
      preview: "I am experiencing swelling again.",
      body: "Hello doctor, since the last visit I'm seeing swelling in my left leg again. Should I come in or take any medication?",
    },
  ];

  function openView(i: number) {
    setViewIndex(i);
    setReplyIndex(null);
  }

  function openReply(i: number) {
    setReplyIndex(i);
    setViewIndex(null);
  }

  function sendReply(i: number) {
    const text = replyText[i] || "";
    // placeholder: in a real app this would call an API
    console.log(`Reply to message ${i}:`, text);
    // clear reply box and close
    setReplyText((s) => ({ ...s, [i]: "" }));
    setReplyIndex(null);
    alert("Reply sent (mock)");
  }

  function sendNewMessage() {
    // mock send for new message
    console.log("Send new message to", newTo, newSubject, newBody);
    setNewTo("");
    setNewSubject("");
    setNewBody("");
    setReplyIndex(null);
    alert("New message sent (mock)");
  }

  return (
    <div className={styles["modal-overlay"]}>
      <div className={styles["modal-container"]} role="dialog" aria-modal="true">
        <div className={styles["modal-header"]}>
          <h3 className={styles["modal-title"]}>Messages</h3>
            <div className={styles.modalHeaderActions}>
              <button
                className={styles.viewAppointmentsBtn}
                onClick={() => {
                  // open composer for a brand new message
                  setViewIndex(null);
                  setReplyIndex(-1);
                  setNewTo("");
                  setNewSubject("");
                  setNewBody("");
                }}
              >
                + New message
              </button>
            </div>
        </div>

        <div className={styles["modal-body"]}>
          <p className={styles.description}>View messages from other doctors, facilities and patients.</p>

          <div className={styles.mt12}>
            {replyIndex === -1 && (
              <NewMessageComposer
                to={newTo}
                subject={newSubject}
                body={newBody}
                setTo={setNewTo}
                setSubject={setNewSubject}
                setBody={setNewBody}
                onCancel={() => setReplyIndex(null)}
                onSend={() => sendNewMessage()}
              />
            )}

            {sample.map((m, i) => (
              <div key={i} className={`${styles.cardSpacing} ${styles.messageCard}`}>
                <div className={styles.viewHeaderRow}>
                  <div>
                    <div className={styles.messageFromName}>{m.from}</div>
                    <div className={styles.messageSubject}>{m.subject}</div>
                  </div>
                    <div className={styles.messageActions}>
                    <div className={styles.messageTimestamp}>2h ago</div>
                    <button className={styles.secondaryBtn} onClick={() => openView(i)}>View</button>
                    <button className={styles.viewAppointmentsBtn} onClick={() => openReply(i)}>Reply</button>
                  </div>
                </div>

                <div className={`${styles.noteText} ${styles.mt8}`}>{m.preview}</div>

                {/* Expanded view */}
                {viewIndex === i && (
                  <div className={`${styles.cardSpacing} ${styles.messageExpanded}`}>
                    <div className={styles.messageBody}>{m.body}</div>
                    <div className={styles.modalFooterActions}>
                      <button className={styles.secondaryBtn} onClick={() => setViewIndex(null)}>Close</button>
                      <button className={styles.viewAppointmentsBtn} onClick={() => { setViewIndex(null); openReply(i); }}>Reply</button>
                    </div>
                  </div>
                )}

                {/* Reply box */}
                {replyIndex === i && (
                  <div className={styles.mt12}>
                    <textarea
                      className={styles.noteTextareaLarge}
                      value={replyText[i] || ""}
                      onChange={(e) => setReplyText((s) => ({ ...s, [i]: e.target.value }))}
                      placeholder={`Reply to ${m.from}...`}
                    />
                    <div className={styles.modalFooterActions}>
                      <button className={styles.secondaryBtn} onClick={() => setReplyIndex(null)}>Cancel</button>
                      <button className={styles.viewAppointmentsBtn} onClick={() => sendReply(i)}>Send</button>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        <div className={styles["modal-footer"]}>
          <div className={styles.modalFooterActions}>
            <button className={styles.secondaryBtn} onClick={() => { setViewIndex(null); setReplyIndex(null); onClose(); }}>Close</button>
            <button className={styles.viewAppointmentsBtn} onClick={() => { setViewIndex(null); setReplyIndex(null); onClose(); }}>Done</button>
          </div>
        </div>
      </div>
    </div>
  );
}

// Composer view for creating a brand-new message (rendered below modal)
function NewMessageComposer({
  to,
  subject,
  body,
  setTo,
  setSubject,
  setBody,
  onCancel,
  onSend,
}: {
  to: string;
  subject: string;
  body: string;
  setTo: (s: string) => void;
  setSubject: (s: string) => void;
  setBody: (s: string) => void;
  onCancel: () => void;
  onSend: () => void;
}) {
  return (
    <div className={`${styles.cardSpacing} ${styles.messageCard}`} style={{ marginTop: 12 }}>
      <label className={styles.formLabel}>To</label>
      <input className={styles.compactSelect} value={to} onChange={(e) => setTo(e.target.value)} placeholder="recipient@example.com" />

      <label className={styles.formLabel} style={{ marginTop: 8 }}>Subject</label>
      <input className={styles.compactSelect} value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="Subject" />

      <label className={styles.formLabel} style={{ marginTop: 8 }}>Message</label>
      <textarea className={styles.noteTextareaLarge} value={body} onChange={(e) => setBody(e.target.value)} />

      <div className={styles.modalFooterActions} style={{ marginTop: 8 }}>
        <button className={styles.secondaryBtn} onClick={onCancel}>Cancel</button>
        <button className={styles.viewAppointmentsBtn} onClick={onSend}>Send</button>
      </div>
    </div>
  );
}

