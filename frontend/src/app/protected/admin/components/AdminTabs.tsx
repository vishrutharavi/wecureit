type Props = {
  active: "doctors" | "facilities";
  onChange: (v: "doctors" | "facilities") => void;
};

export default function AdminTabs({ active, onChange }: Props) {
  return (
    <div style={{ marginTop: "1.5rem", display: "flex", gap: "0.75rem" }}>
      {(() => {
        const tabs = ["doctors", "facilities"] as const;
        return tabs.map((tab) => (
        <button
          key={tab}
          onClick={() => onChange(tab)}
          style={{
            padding: "0.5rem 1rem",
            borderRadius: "999px",
            border: "1px solid #fecaca",
            background:
              active === tab ? "#fee2e2" : "white",
            color: "#ef4444",
            fontWeight: active === tab ? 700 : 500,
            cursor: "pointer",
          }}
        >
          {tab.charAt(0).toUpperCase() + tab.slice(1)}
        </button>
      ))
      })()}
    </div>
  );
}
