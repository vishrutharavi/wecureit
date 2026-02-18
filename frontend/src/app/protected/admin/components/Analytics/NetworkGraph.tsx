"use client";

import React, { useEffect, useState, useCallback, useRef } from "react";
import { auth } from "@/lib/firebase";
import { getReferralPatterns } from "@/lib/admin/adminApi";

type Pattern = { fromDoctorName: string; toDoctorName: string; speciality: string; frequency: number };

type NodePos = { name: string; x: number; y: number };

const W = 680;
const H = 560;
const CX = W / 2;
const CY = H / 2;
const R_OUTER = 220;

function buildLayout(names: string[]): NodePos[] {
  if (names.length === 0) return [];
  if (names.length === 1) return [{ name: names[0], x: CX, y: CY }];
  return names.map((name, i) => {
    const angle = (2 * Math.PI * i) / names.length - Math.PI / 2;
    return { name, x: CX + R_OUTER * Math.cos(angle), y: CY + R_OUTER * Math.sin(angle) };
  });
}

export default function NetworkGraph() {
  const [patterns, setPatterns] = useState<Pattern[]>([]);
  const [loading, setLoading]   = useState(true);
  const [filter, setFilter]     = useState("");
  const [selected, setSelected] = useState<string | null>(null);
  const [tooltip, setTooltip]   = useState<{ x: number; y: number; text: string } | null>(null);
  const svgRef = useRef<SVGSVGElement>(null);

  const fetchPatterns = useCallback(async () => {
    const user = auth.currentUser;
    if (!user) return;
    try {
      setLoading(true);
      const token = await user.getIdToken();
      const data = await getReferralPatterns(token);
      if (Array.isArray(data)) setPatterns(data as Pattern[]);
    } catch { /* ignore */ }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchPatterns(); }, [fetchPatterns]);

  // Derive unique node names, filtered
  const allNames = Array.from(new Set(
    patterns.flatMap((p) => [p.fromDoctorName, p.toDoctorName])
  ));
  const filteredNames = filter.trim()
    ? allNames.filter((n) => n.toLowerCase().includes(filter.toLowerCase()))
    : allNames;
  const nodeMap = new Map(buildLayout(filteredNames).map((n) => [n.name, n]));

  // Filter patterns to only those with both endpoints visible
  const visiblePatterns = patterns.filter(
    (p) => nodeMap.has(p.fromDoctorName) && nodeMap.has(p.toDoctorName)
  );

  const maxFreq = Math.max(1, ...visiblePatterns.map((p) => p.frequency));

  function edgeOpacity(freq: number) { return 0.2 + (freq / maxFreq) * 0.65; }
  function edgeWidth(freq: number)   { return 1 + (freq / maxFreq) * 4; }

  function isHighlighted(from: string, to: string) {
    if (!selected) return true;
    return from === selected || to === selected;
  }

  if (loading) {
    return <div style={{ textAlign: "center", padding: "4rem", color: "#9ca3af" }}>Loading network…</div>;
  }

  return (
    <div style={{ marginTop: "2rem" }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "1.25rem" }}>
        <div>
          <h2 style={{ margin: 0, fontSize: "1.4rem", fontWeight: 800, color: "#7f1d1d" }}>Referral Network</h2>
          <div style={{ fontSize: "0.8rem", color: "#9ca3af", marginTop: 4 }}>
            {allNames.length} doctors · {patterns.length} referral patterns
          </div>
        </div>
        <div style={{ display: "flex", gap: "0.75rem", alignItems: "center" }}>
          <input
            value={filter}
            onChange={(e) => { setFilter(e.target.value); setSelected(null); }}
            placeholder="Filter by doctor name…"
            style={{
              padding: "0.45rem 0.85rem", borderRadius: 8, border: "1px solid #fecaca",
              fontSize: "0.875rem", outline: "none", width: 220,
            }}
          />
          {selected && (
            <button
              onClick={() => setSelected(null)}
              style={{ padding: "0.45rem 0.85rem", borderRadius: 8, border: "1px solid #fecaca", background: "white", color: "#ef4444", cursor: "pointer", fontSize: "0.875rem" }}
            >
              Clear selection
            </button>
          )}
        </div>
      </div>

      <div style={{ background: "white", borderRadius: 12, border: "1px solid #fecaca", overflow: "hidden", position: "relative" }}>
        {filteredNames.length === 0 ? (
          <div style={{ textAlign: "center", padding: "4rem", color: "#9ca3af" }}>
            {patterns.length === 0
              ? "No referral patterns yet. Create multiple referrals between the same doctors."
              : "No doctors match your filter."}
          </div>
        ) : (
          <>
            <svg ref={svgRef} width="100%" viewBox={`0 0 ${W} ${H}`} style={{ display: "block" }}>
              <defs>
                <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="5" markerHeight="5" orient="auto">
                  <path d="M 0 0 L 10 5 L 0 10 z" fill="#ef4444" fillOpacity={0.6} />
                </marker>
              </defs>

              {/* Edges */}
              {visiblePatterns.map((p, i) => {
                const from = nodeMap.get(p.fromDoctorName)!;
                const to   = nodeMap.get(p.toDoctorName)!;
                const highlighted = isHighlighted(p.fromDoctorName, p.toDoctorName);
                // Shorten edge so arrow doesn't overlap node circle
                const dx = to.x - from.x;
                const dy = to.y - from.y;
                const dist = Math.sqrt(dx * dx + dy * dy) || 1;
                const nodeR = 20;
                const x2 = to.x - (dx / dist) * (nodeR + 6);
                const y2 = to.y - (dy / dist) * (nodeR + 6);
                return (
                  <line
                    key={i}
                    x1={from.x} y1={from.y} x2={x2} y2={y2}
                    stroke="#ef4444"
                    strokeWidth={edgeWidth(p.frequency)}
                    strokeOpacity={highlighted ? edgeOpacity(p.frequency) : 0.05}
                    markerEnd="url(#arrow)"
                    style={{ cursor: "pointer", transition: "stroke-opacity 0.2s" }}
                    onMouseEnter={(e) => {
                      const rect = svgRef.current?.getBoundingClientRect();
                      if (rect) setTooltip({
                        x: e.clientX - rect.left + 10,
                        y: e.clientY - rect.top - 30,
                        text: `${p.fromDoctorName} → ${p.toDoctorName} · ${p.speciality} · ×${p.frequency}`,
                      });
                    }}
                    onMouseLeave={() => setTooltip(null)}
                  />
                );
              })}

              {/* Nodes */}
              {buildLayout(filteredNames).map((node) => {
                const isSelected = selected === node.name;
                const isConnected = selected
                  ? visiblePatterns.some((p) => p.fromDoctorName === node.name || p.toDoctorName === node.name
                      ? (p.fromDoctorName === selected || p.toDoctorName === selected)
                      : false)
                  : true;
                const dimmed = selected && !isSelected && !isConnected;
                // Short label: first name only
                const shortLabel = node.name.split(" ").slice(0, 2).join(" ");
                return (
                  <g key={node.name} style={{ cursor: "pointer" }} onClick={() => setSelected(isSelected ? null : node.name)}>
                    <circle
                      cx={node.x} cy={node.y} r={20}
                      fill={isSelected ? "#ef4444" : "#fff5f5"}
                      stroke={isSelected ? "#b91c1c" : "#fca5a5"}
                      strokeWidth={isSelected ? 2.5 : 1.5}
                      fillOpacity={dimmed ? 0.3 : 1}
                      strokeOpacity={dimmed ? 0.2 : 1}
                      style={{ transition: "all 0.2s" }}
                    />
                    <text
                      x={node.x} y={node.y + 34}
                      textAnchor="middle"
                      fontSize={10}
                      fontWeight={isSelected ? 700 : 500}
                      fill={dimmed ? "#d1d5db" : "#374151"}
                      style={{ pointerEvents: "none", userSelect: "none" }}
                    >
                      {shortLabel}
                    </text>
                  </g>
                );
              })}
            </svg>

            {/* Tooltip */}
            {tooltip && (
              <div style={{
                position: "absolute", left: tooltip.x, top: tooltip.y,
                background: "rgba(0,0,0,0.75)", color: "white",
                borderRadius: 6, padding: "0.3rem 0.6rem", fontSize: "0.75rem",
                pointerEvents: "none", whiteSpace: "nowrap", zIndex: 10,
              }}>
                {tooltip.text}
              </div>
            )}

            {/* Legend */}
            <div style={{ padding: "0.75rem 1.5rem", borderTop: "1px solid #f3f4f6", display: "flex", gap: "1.5rem", fontSize: "0.75rem", color: "#9ca3af" }}>
              <span>Thicker edge = more referrals</span>
              <span>Click a node to highlight connections</span>
              <span style={{ marginLeft: "auto" }}>
                Showing {filteredNames.length} doctors · {visiblePatterns.length} connections
              </span>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
