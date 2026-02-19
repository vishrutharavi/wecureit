# Branch & Bound Slot Selection: Implementation, Priorities, and Example

## Overview
The system implements a **branch-and-bound (B&B) algorithm** to find the 5 optimal appointment slots from thousands of candidates across a date range. B&B prunes the search tree using upper-bound scores, only exploring branches that could yield better solutions than currently found.

---

## Algorithm Architecture

### Phase 1: Initialization (Candidate Generation)
For each date in the range:
1. Fetch all 15-minute availability slots from `PatientBookingService`
2. Filter valid (AVAILABLE) slots
3. Detect schedule gaps between existing appointments
4. Build `SlotSearchNode` for each valid candidate
5. Compute **upper-bound score** (`boundScore`) for pruning
6. Add to priority queue (sorted by boundScore, descending)

**Result:** Queue with potentially thousands of candidates, each with an optimistic score.

### Phase 2: Best-First Search with Pruning
```
while queue is not empty AND explored < 500 nodes AND time < 5 seconds:
    node = queue.poll()  // highest bound score first
    
    // Pruning: if this node's upper bound ≤ current worst-in-top-5, skip
    if node.boundScore ≤ worstInTopK.actualScore:
        continue
    
    // Compute actual score
    actualScore = score(gap-fill=50%, fragmentation=30%, temporal=20%)
    
    // Keep top 5
    if topK.size < 5:
        topK.add(node)
    else if actualScore > worstInTopK.actualScore:
        topK.remove(worst)
        topK.add(node)
    
    // Early termination if all 5 are excellent (≥80 score)
    if topK.all(score ≥ 80):
        break
```

**Key Insight:** B&B explores high-potential nodes first; if a node's optimistic upper bound is worse than the current worst solution, pruning stops all branches under it.

### Phase 3: Deduplication & Output
- Sort top 5 by actual score (descending)
- Remove time-overlapping duplicates
- Convert to user-friendly `SlotSuggestion` DTOs

---

## Scoring Formula (Weighted Composite)

```
Total Score = 0.5 × GapFill + 0.3 × Fragmentation + 0.2 × Temporal
```

### 1. Gap-Fill Efficiency (50% Priority) — **MOST IMPORTANT**

Measures how well a slot "fills" existing gaps in the schedule.

| Scenario | Score | Reason |
|----------|-------|--------|
| Perfect fill (slot exactly matches gap start & end) | **100** | Completely efficient |
| Fills gap start OR end cleanly | **80** | One boundary aligned |
| Within a gap, remainder ≥60 min | **70** | Creates larger usable gaps |
| Within a gap, remainder 30-60 min | **60** | Medium-sized remainder |
| Within a gap, remainder 15-30 min | **40** | Small remainder (marginal) |
| Within a gap, remainder <15 min | **20** | Tiny unusable gap (fragmentation) |
| Adjacent to existing appointment | **30** | Fills slot but no gap efficiency |
| Empty schedule (no appointments) | **50** | No gap efficiency learned |
| Random/orphaned slot | **10** | No clear benefit |

---

### 2. Fragmentation Penalty (30% Priority)

Discourages slots that create "split" gaps too small to be useful (< 15 min).

**Penalty Logic:**
```
for each gap created after inserting the slot:
    if gap < 15 min: penalty -= 50.0
    else if gap < 30 min: penalty -= 20.0
    else if gap < 60 min: penalty -= 10.0
```

**Why it matters:** If inserting a 30-min appointment splits a 45-min gap into two 7.5-min pieces, neither is usable, so the penalty is -50 × 2 = **-100 points**.

---

### 3. Temporal Preference (20% Priority)

Encourages earlier dates and morning hours.

**Date Scoring:**
```
daysFromNow = days between today and candidate
dateScore = max(0, 100 × (1 - daysFromNow / 14))
// Examples:
// Today: 100 points
// 7 days out: 50 points
// 14+ days: 0 points
```

**Time of Day Scoring:**
```
if 9 AM–12 PM: timeScore = 10
else if 12 PM–5 PM: timeScore = 5
else: timeScore = 0
```

**Example:** 
- Slot at 10:30 AM, 3 days away → dateScore = 100×(1 - 3/14) ≈ 78.6, timeScore = 10 → **Temporal ≈ 88.6**

---

## Upper Bound (For Branch & Bound Pruning)

```
BoundScore = 0.5 × 100 + 0.3 × 0 + 0.2 × TemporalScore
           = 50 + 0 + 0.2 × TemporalScore
           = up to 50 + 20 = 70
```

This assumes **every** slot perfectly fills a gap and creates **no** fragmentation — an optimistic estimate. If this is ≤ worst current solution (e.g., 65), pruning eliminates the branch.

---

## Critical: Break Rule Enforcement (BEFORE B&B Scoring)

**Break Rule:** Doctor cannot work **more than 60 minutes continuously** (i.e., `> 60` is violation; exactly 60 is allowed).

A 15-minute gap = valid break boundary (resets continuous counter).

`PatientBookingService.getAvailabilitySlots()` enforces this UPSTREAM of B&B:

```java
// For each candidate slot, simulate insertion and check continuous work
for var hypo : appointments {
    if (groupContainingHypo > 60 minutes) {  // STRICTLY greater than 60
        status = "UNAVAILABLE"  // Slot rejected before B&B sees it
    }
}
```

**Examples:**
- 60 min continuous → ALLOWED ✓
- 61 min continuous → REJECTED ✗
- 59 min continuous → ALLOWED ✓

**Result:** Only slots respecting the break rule are marked AVAILABLE and reach B&B. Invalid slots are pre-filtered out.

---

## Example Walkthrough

### Scenario
**Doctor's existing schedule on 2026-02-23:**
- 9:00–9:30 (appointment A)
- 11:00–11:30 (appointment B)

**Office hours:** 8:00 AM–5:00 PM

**Gaps detected:**
1. 8:00–9:00 (60 min)
2. 9:30–11:00 (90 min) 
3. 11:30–5:00 PM (330 min)

**Requesting:** 30-minute appointment

---

### Candidate Slots & Scoring

#### **Slot 1: 8:00–8:30 (fills start of 60-min morning gap)**

**Break rule check:**
- Only appointment A (9:00–9:30) comes after, gap = 30 min (≥ 15, valid break)
- ✓ Passes break enforcement → Status: AVAILABLE

**Gap-Fill (50%):**
- Fills start of 60-min gap → 80 points
- After insertion: 8:30–9:00 remains (30 min)
- **Gap-Fill = 80**

**Fragmentation (30%):**
- Remaining gap = 30 min → no penalty
- **Fragmentation penalty = 0**

**Temporal (20%):**
- Today, 8:00 AM (early morning, before preferred 9 AM)
- dateScore = 100, timeScore = 0 (before 9 AM window)
- **Temporal ≈ 20** (lower value for early morning)

**Total Score = 0.5(80) + 0.3(0) + 0.2(20) = 40 + 0 + 4 = 44**

---

#### **Slot 2: 10:00–10:30 (within 90-min mid-morning gap)**

**Break rule check:**
- Before: appointment A ends 9:30, new slot starts 10:00 → gap = 30 min (valid break ✓)
- After: new slot ends 10:30, appointment B starts 11:00 → gap = 30 min (valid break ✓)
- ✓ Passes break enforcement → Status: AVAILABLE

**Gap-Fill (50%):**
- Within gap (not aligned to boundaries) → 40 points (remainder 30–60 min: 9:30–10:00 before, 10:30–11:00 after)
- **Gap-Fill = 40**

**Fragmentation (30%):**
- Before insertion: one 90-min gap (9:30–11:00)
- After insertion: two 30-min gaps (9:30–10:00 and 10:30–11:00)
- Both gaps = 30 min (≥ 30, usable)
- **Fragmentation penalty = 0**

**Temporal (20%):**
- Today, 10:00 AM (peak morning hours)
- dateScore = 100, timeScore = 10
- **Temporal = 110**

**Total Score = 0.5(40) + 0.3(0) + 0.2(110) = 20 + 0 + 22 = 42**

---

#### **Slot 3: 9:30–10:00 (passes break rule — exactly 60 min continuous)**

**Break rule check:**
- Contiguous group: Appt A (9:00-9:30, 30 min) + gap 0 + Slot (9:30-10:00, 30 min) = **60 min exactly**
- Rule: continuous work must be **strictly > 60** for violation (exactly 60 is allowed)
- Violation? `60 > 60`? **NO** → ✓ Passes
- ✓ Status: AVAILABLE

**Gap-Fill (50%):**
- Fills start boundary of 90-min gap (9:30-11:00) → 100 points (perfect boundary align)
- **Gap-Fill = 100**

**Fragmentation (30%):**
- Remaining after slot: 10:00-11:00 = 60 min (highly usable)
- **Fragmentation penalty = 0**

**Temporal (20%):**
- Today, 9:30 AM (within 9 AM-12 PM preferred window)
- dateScore = 100, timeScore = 10
- **Temporal = 110**

**Total Score = 0.5(100) + 0.3(0) + 0.2(110) = 50 + 0 + 22 = 72**

**→ TOP CANDIDATE by B&B ranking**

---

#### **Slot 4: 12:00 PM–12:30 PM (within large 330-min afternoon gap)**

**Break rule check:**
- Before: appointment B ends 11:30, new slot starts 12:00 → gap = 30 min (valid break ✓)
- After: slot ends 12:30, office still open
- ✓ Passes break enforcement → Status: AVAILABLE

**Gap-Fill (50%):**
- Within gap start (11:30), but doesn't align boundary
- Remaining after insertion: 12:30–5:00 PM = 270 min (≥ 60)
- **Gap-Fill = 70**

**Fragmentation (30%):**
- Remaining gap = 270 min → no penalty
- **Fragmentation = 0**

**Temporal (20%):**
- Today, 12:00 PM (afternoon, lower preference)
- dateScore = 100, timeScore = 5
- **Temporal = 105**

**Total = 0.5(70) + 0.3(0) + 0.2(105) = 35 + 0 + 21 = 56**

---

#### **Slot 5: 8:30–9:00 (passes break rule — exactly 60 min continuous)**

**Break rule check:**
- Contiguous group: Slot (8:30-9:00, 30 min) + gap 0 + Appt A (9:00-9:30, 30 min) = **60 min exactly**
- Rule: continuous work must be **strictly > 60** for violation (exactly 60 is allowed)
- Violation? `60 > 60`? **NO** → ✓ Passes
- ✓ Status: AVAILABLE

**Gap-Fill (50%):**
- Fills end boundary of 60-min morning gap (8:00-9:00) → 100 points (perfect boundary align)
- **Gap-Fill = 100**

**Fragmentation (30%):**
- Remaining before slot: 8:00-8:30 = 30 min (usable)
- **Fragmentation penalty = 0**

**Temporal (20%):**
- Today, 8:30 AM (before preferred 9 AM window)
- dateScore = 100, timeScore = 0 (< 9 AM)
- **Temporal = 20** (lower for early slot)

**Total Score = 0.5(100) + 0.3(0) + 0.2(20) = 50 + 0 + 4 = 54**

---

### Final Ranking (ALL slots pass break enforcement)
1. **Slot 3 (9:30–10:00)** — Score 72 ✓✓✓ **BEST** — Perfect gap fill + peak morning hours
2. **Slot 4 (12:00 PM–12:30 PM)** — Score 56 — Good gap fill but afternoon slot (lower temporal score)
3. **Slot 5 (8:30–9:00)** — Score 54 — Perfect gap fill but very early morning (before 9 AM window)
4. **Slot 1 (8:00–8:30)** — Score 44 — Good gap fill but earliest morning (lower temporal)
5. **Slot 2 (10:00–10:30)** — Score 42 — Mid-morning but creates split gaps (40 pt gap-fill, not perfect boundary)

**Key insight:** The rule allows exactly 60 minutes of continuous work. Slots 3 and 5, though back-to-back with appointments (0 gap), are VALID because the total contiguous time = exactly 60 min, not > 60. B&B ranks Slot 3 as optimal because it combines perfect gap-fill efficiency (100 pts) with peak morning preference (110 pts).

---

## Architecture Flow Diagram

```
1. PatientBookingService.getAvailabilitySlots()
   ├─ For each candidate slot:
   │  ├─ Check for appointment overlap → BOOKED
   │  ├─ Simulate continuous work with break rule
   │  └─ If continuous > 60 min → UNAVAILABLE (filtered here!)
   │
   └─ Return only AVAILABLE slots

2. OptimalSlotSuggestionService.buildCandidatesForDate()
   ├─ Filter for status = "AVAILABLE" (break rule pre-enforced!)
   ├─ Detect gaps in schedule
   └─ Add to priority queue with boundScore

3. B&B Search (Phase 2)
   ├─ Explore high-boundScore candidates first
   ├─ Compute actual score: 50% gap-fill + 30% frag + 20% temporal
   ├─ Prune low-potential branches
   └─ Return top 5 scored slots
```

---

## Key Design Insights

### **What Problem is B&B Solving Primarily?**

**Gap-fill efficiency (50 weight)** is the DOMINANT optimization. The algorithm fundamentally answers:

> *"How can we slot new appointments such that they fill existing schedule gaps efficiently, without creating too much wasted micro-gaps?"*

**Why?**
- **Scheduling efficiency:** Hospitals/clinics want dense, compact schedules without many split unusable gaps
- **Doctor utilization:** Reduces idle time and improves room turnover
- **Patient convenience:** Earlier/denser schedules often mean better appointment availability

### **Secondary Concerns:**

- **Fragmentation (30%):** Prevents the "Swiss cheese" schedule (many 5–10 min gaps that can't fit new requests)
- **Temporal (20%):** User preference — earlier dates are better; mornings slightly better than afternoons

### **Break Rule Enforcement:**

- **Executed upstream in PatientBookingService**, not in B&B
- Ensures only compliant candidates reach B&B
- Prevents invalid slots from being scored and returned

### **B&B Advantage:**

- Explores ~500 nodes in 5 seconds (vs. 2000+ if brute-force checked all)
- Prioritizes high-potential branches early (bound-based ordering)
- Stops exploring pessimistic branches (actual score ≤ worst-in-top-5)
- Guarantees finding near-optimal or optimal solution within time/exploration limits

---

## Configuration Constants

| Parameter | Value | Impact |
|-----------|-------|--------|
| `MAX_NODES_EXPLORED` | 500 | Search breadth limit |
| `MAX_SEARCH_TIME_MS` | 5000 | 5-second timeout |
| `MAX_SUGGESTIONS` | 5 | Top-K results |
| `EXCELLENT_SCORE_THRESHOLD` | 80 | Early termination threshold |
| `GAP_MIN` | 15 | Minimum valid break gap (minutes) to separate contiguous groups |
| `MAX_CONTINUOUS_MINUTES` | 60 | Violation only if continuous work **strictly > 60** min (exactly 60 allowed) |

---

## Summary

**System Architecture:**
1. **Break enforcement (upstream):** PatientBookingService marks slots UNAVAILABLE if they violate `continuous_work > 60` limit (exactly 60 is allowed)
2. **Gap detection:** ScheduleGapAnalyzer identifies idle time windows
3. **B&B optimization (downstream):** OptimalSlotSuggestionService scores valid candidates on gap-fill (50%) > fragmentation (30%) > temporal preference (20%)

**Result:**
- Only slots respecting the break rule are considered
- Among valid slots, the algorithm prioritizes **schedule compactness** (gap-fill first)
- Returns top 5 non-overlapping suggestions with explainable reasons

---

## Important: Break Rule Clarification

| Scenario | Valid? |
|----------|--------|
| Continuous work = 60 min | ✓ YES (exactly 60 allowed) |
| Continuous work = 61 min | ✗ NO (violation) |
| Continuous work = 59 min | ✓ YES |
| Gap between appointments = 15 min | ✓ YES (breaks contiguous group) |
| Gap between appointments = 14 min | ✗ NO (still continuous) |

**Code check:** The implementation uses `if (cum > MAX_CONTINUOUS_MINUTES)` where `MAX_CONTINUOUS_MINUTES = 60L`. This means exactly 60 is allowed; only > 60 triggers unavailability.
