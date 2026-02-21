"use client";

import React from "react";
import styles from "./bookingCopilot.module.scss";

type Props = {
  utterance: string;
  loading: boolean;
  error: string | null;
  samplePrompts: string[];
  onChange: (value: string) => void;
  onSubmit: () => void;
  onClear: () => void;
  onPickPrompt: (value: string) => void;
};

export default function CopilotInput({
  utterance,
  loading,
  error,
  samplePrompts,
  onChange,
  onSubmit,
  onClear,
  onPickPrompt,
}: Props) {
  return (
    <div className={styles.inputRow}>
      <textarea
        className={styles.textarea}
        placeholder="Example: I need a dermatology appointment next week after 4pm for 30 minutes at Downtown Clinic."
        value={utterance}
        onChange={(e) => onChange(e.target.value)}
      />

      <div className={styles.actions}>
        <button className={styles.primaryButton} onClick={onSubmit} disabled={loading}>
          {loading ? "Thinking..." : "Find slots"}
        </button>
        <button className={styles.ghostButton} onClick={onClear} type="button">
          Clear
        </button>
      </div>

      <div className={styles.hint}>Tip: try “next week”, “after 4pm”, or a clinic name.</div>

      <div className={styles.chips}>
        {samplePrompts.map((prompt) => (
          <button
            key={prompt}
            type="button"
            className={styles.chip}
            onClick={() => onPickPrompt(prompt)}
          >
            {prompt}
          </button>
        ))}
      </div>

      {error && <div className={styles.error}>{error}</div>}
    </div>
  );
}