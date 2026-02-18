export type AdminTab = "doctors" | "facilities" | "analytics";
export type AnalyticsSubTab = "overview" | "alerts" | "network";

type Props = {
  active: AdminTab;
  onChange: (v: AdminTab) => void;
  analyticsSubTab: AnalyticsSubTab;
  onAnalyticsSubTabChange: (v: AnalyticsSubTab) => void;
};

const ANALYTICS_SUB_LABELS: Record<AnalyticsSubTab, string> = {
  overview: "Overview",
  alerts: "Alerts",
  network: "Network Graph",
};

export default function AdminTabs({ active, onChange, analyticsSubTab, onAnalyticsSubTabChange }: Props) {
  return (
    <div>
      {/* Main tab row */}
      <div style={{ marginTop: "1.5rem", display: "flex", alignItems: "center", gap: "0.75rem" }}>
        {/* Left group: management tabs */}
        <div style={{ display: "flex", gap: "0.75rem", flexGrow: 1 }}>
          {(["doctors", "facilities"] as AdminTab[]).map((tab) => (
            <button
              key={tab}
              onClick={() => onChange(tab)}
              style={{
                padding: "0.5rem 1rem",
                borderRadius: "999px",
                border: "1px solid #fecaca",
                background: active === tab ? "#fee2e2" : "white",
                color: "#ef4444",
                fontWeight: active === tab ? 700 : 500,
                cursor: "pointer",
              }}
            >
              {tab === "doctors" ? "Doctors" : "Facilities"}
            </button>
          ))}
        </div>

        {/* Right group: Referral Analytics */}
        <button
          onClick={() => onChange("analytics")}
          style={{
            padding: "0.5rem 1.25rem",
            borderRadius: "999px",
            border: active === "analytics" ? "2px solid #ef4444" : "1px solid #fecaca",
            background: active === "analytics" ? "#ef4444" : "white",
            color: active === "analytics" ? "white" : "#ef4444",
            fontWeight: 700,
            cursor: "pointer",
          }}
        >
          Referral Analytics
        </button>
      </div>

      {/* Sub-tab row — only shown when analytics is active */}
      {active === "analytics" && (
        <div style={{
          marginTop: "0.75rem",
          display: "flex",
          gap: "0.5rem",
          paddingLeft: "0.25rem",
        }}>
          {(Object.keys(ANALYTICS_SUB_LABELS) as AnalyticsSubTab[]).map((sub) => (
            <button
              key={sub}
              onClick={() => onAnalyticsSubTabChange(sub)}
              style={{
                padding: "0.35rem 0.9rem",
                borderRadius: "999px",
                border: analyticsSubTab === sub ? "1.5px solid #ef4444" : "1px solid #fecaca",
                background: analyticsSubTab === sub ? "#fff5f5" : "white",
                color: analyticsSubTab === sub ? "#ef4444" : "#9ca3af",
                fontWeight: analyticsSubTab === sub ? 700 : 500,
                fontSize: "0.85rem",
                cursor: "pointer",
              }}
            >
              {ANALYTICS_SUB_LABELS[sub]}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
