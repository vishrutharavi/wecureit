"use client";

import React from "react";
import { apiFetch, showInlineToast } from '@/lib/api';
import { useRouter } from "next/navigation";
import styles from "../../patient.module.scss";
import DatePickerGrid from "./DatePickerGrid";

type Selection = {
  doctor?: { id: string; name: string } | null;
  facility?: { id: string; name: string } | null;
  specialty?: { id: string; name: string } | null;
};

// Availability response shape returned by /api/doctors/:id/availability
type AvailabilityResp = {
  workDate: string;
  facilityId?: string;
  facilityName?: string;
  bookable?: boolean;
  isBookable?: boolean;
  // time window for the availability row (HH:MM 24h)
  startTime?: string;
  endTime?: string;
};

export default function DateAndTimeSelection() {
  const router = useRouter();
  const [selection, setSelection] = React.useState<Selection | null>(null);
  const [availableDates, setAvailableDates] = React.useState<string[] | null>(null);
  const [availabilities, setAvailabilities] = React.useState<AvailabilityResp[] | null>(null);
  // date is null until user explicitly picks one from the mini calendar
  const [date, setDate] = React.useState<string | null>(null);
  const [duration, setDuration] = React.useState<number | null>(null);
  const [selectedTime, setSelectedTime] = React.useState<string | null>(null);

  const computeRange = (timeLabel: string | null, durationMin: number | null) => {
    if (!timeLabel || !date || !durationMin) return null;
    const startDate = new Date(date + 'T00:00:00');
    const match = timeLabel.match(/(\d{1,2}):(\d{2})\s?(AM|PM)/i);
    if (!match) return null;
    let h = parseInt(match[1], 10);
    const m = parseInt(match[2], 10);
    const ampmStr = (match[3] || '').toUpperCase();
    const isPM = ampmStr === 'PM';
    if (h === 12) h = isPM ? 12 : 0;
    if (isPM && h !== 12) h += 12;
    startDate.setHours(h, m, 0, 0);
    const endDate = new Date(startDate.getTime() + durationMin * 60000);
    const startLabel = startDate.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
    const endLabel = endDate.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
    return { startLabel, endLabel };
  };

  React.useEffect(() => {
    try {
      const raw = sessionStorage.getItem("bookingSelection");
      if (raw) setSelection(JSON.parse(raw));
    } catch {
      // ignore
    }
  }, []);

  // fetch doctor availabilities (next 30 days) and compute available dates for the selected facility
  React.useEffect(() => {
    let canceled = false;
    (async () => {
      if (!selection || !selection.doctor?.id) {
        setAvailableDates(null);
        return;
      }
      try {
        const doctorId = selection.doctor.id;
        const from = new Date();
        const to = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000);
        const fromIso = from.toISOString().slice(0, 10);
        const toIso = to.toISOString().slice(0, 10);
        const url = `/api/doctors/${doctorId}/availability?from=${fromIso}&to=${toIso}`;
        const resp = await apiFetch(url) as AvailabilityResp[];
        if (canceled) return;

    // availability fetch completed

        if (!Array.isArray(resp)) {
          setAvailableDates(null);
          return;
        }

        // filter by facility if present, and only include bookable entries
        const filtered = resp.filter((r: AvailabilityResp & { facilityName?: string; facilityId?: string }) => {
          if (!r) return false;
          if (!r.bookable && !r.isBookable) return false;
          if (selection.facility?.id) {
            // first try matching facilityId if present
            const facIdResp = r.facilityId;
            if (facIdResp) {
              return String(facIdResp || '').trim() === String(selection.facility.id || '').trim();
            }
            // fallback: match by facilityName if facilityId not provided by the API
            const facNameResp = r.facilityName;
            if (facNameResp && selection.facility?.name) {
              return String(facNameResp).trim().toLowerCase() === String(selection.facility.name).trim().toLowerCase();
            }
            return false;
          }
          return true;
        });

        const dates = Array.from(new Set(filtered.map((r) => r.workDate))).sort();
  // filtered availability computed
        setAvailableDates(dates);
        setAvailabilities(filtered);
      } catch (err) {
        console.error('Failed to load availabilities', err);
        showInlineToast('Failed to load doctor availability');
        setAvailableDates(null);
        setAvailabilities(null);
      }
    })();
    return () => { canceled = true; };
  }, [selection]);

  // when date changes, clear duration and selected time so the user chooses anew
  React.useEffect(() => {
    setDuration(null);
    setSelectedTime(null);
  }, [date]);

  const generateTimeSlots = React.useMemo(() => {
    if (!date) return [] as string[];
    // if we have availabilities for the selected date, generate slots from those intervals
    const rows = (availabilities || []).filter((r) => r.workDate === date && (r.bookable || r.isBookable));
    const slotsSet = new Set<number>();
    const granularity = 15; // minutes

    // helper: parse HH:MM to minutes since midnight
    const parseHM = (s: string) => {
      if (!s) return null;
      const m = s.match(/^(\d{1,2}):(\d{2})$/);
      if (!m) return null;
  const hh = parseInt(m[1], 10);
      const mm = parseInt(m[2], 10);
      return hh * 60 + mm;
    };

    // helper: format minutes to 'h:mm AM/PM'
    const fmt = (minutes: number) => {
      const hh = Math.floor(minutes / 60);
      const mm = minutes % 60;
      const period = hh >= 12 ? 'PM' : 'AM';
      let hour = hh % 12;
      if (hour === 0) hour = 12;
      return `${hour}:${String(mm).padStart(2, '0')} ${period}`;
    };

    for (const r of rows) {
      const s = parseHM(r.startTime || '');
      const e = parseHM(r.endTime || '');
      if (s == null || e == null) continue;
      // need enough room for duration; duration may be null until user picks it; if duration null, include all starts
      const dur = duration || 15;
      const lastStart = e - dur;
      for (let t = s; t <= lastStart; t += granularity) {
        slotsSet.add(t);
      }
    }

    const sorted = Array.from(slotsSet).sort((a, b) => a - b);
    const labels = sorted.map((m) => fmt(m));
    return labels;
  }, [date, availabilities, duration]);

  if (!selection) {
    return (
      <div className={styles.wrapper}>
        <div className={styles.bookingHeader}>
          <a onClick={() => router.push('/protected/patient?tab=dropdownselection')} className={styles.backLink}>← Back to Selection</a>
          <h1 className={styles.bookingTitle}>Select Date & Time</h1>
          <div className={styles.subtitle}>Choose your preferred appointment date and time</div>
        </div>

        <div style={{ marginTop: 24 }}>
          <div className={styles.panelWhite}>
            <div className={styles.sectionTitle}>No selection found</div>
            <div className={styles.sectionSubtitle}>Please return to the selection step and choose a doctor, facility and specialty.</div>
            <div style={{ marginTop: 12 }}>
              <button className={styles.viewAppointmentsBtn} onClick={() => router.push('/protected/patient?tab=dropdownselection')}>Back to selection</button>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.wrapper}>
      <div className={styles.bookingHeader}>
        <a onClick={() => router.push('/protected/patient?tab=dropdownselection')} className={styles.backLink}>← Back to Selection</a>
        <h1 className={styles.bookingTitle}>Select Date & Time</h1>
        <div className={styles.subtitle}>Choose your preferred appointment date and time</div>
      </div>

      <div className={styles.bookingGrid}>
        <div>
          <div className={styles.panelWhite}>
            <div className={styles.sectionTitle}>Select Date</div>
            <div className={styles.sectionSubtitle}>Available dates are highlighted</div>

            <div style={{ marginTop: 18 }}>
              <DatePickerGrid value={date} onChange={(iso) => setDate(iso)} availableDates={availableDates} />
            </div>

            {/* debug UI removed */}

            {/* Duration only visible after a date is chosen */}
            {date ? (
              <div style={{ marginTop: 18 }}>
                <div className={styles.sectionSubtitle}>Select Appointment Duration</div>
                <div className={styles.durationGrid}>
                  {[15, 30, 60].map((d) => (
                    <button
                      key={d}
                      onClick={() => {
                        setDuration(d);
                        // clear any previously selected time when duration changes
                        setSelectedTime(null);
                      }}
                      className={d === duration ? `${styles.durationBtn} ${styles.durationBtnActive}` : styles.durationBtn}
                    >
                      {d} mins
                    </button>
                  ))}
                </div>

                {/* Time slots only visible after a duration is chosen */}
                {duration ? (
                  <div style={{ marginTop: 18 }}>
                    <div className={styles.sectionSubtitle}>Available Time Slots</div>
                    <div className={styles.timeSlotsGrid}>
                      {generateTimeSlots.map((t, idx) => {
                        // compute whether this slot falls within the selected time block
                        let inBlock = false;
                        if (selectedTime && duration) {
                          const startIndex = generateTimeSlots.indexOf(selectedTime);
                          const blockCount = Math.ceil(duration / 15);
                          if (startIndex >= 0 && idx >= startIndex && idx < startIndex + blockCount) inBlock = true;
                        }
                        // determine if this slot is a valid start (enough slots remain for the duration)
                        const blockCount = duration ? Math.ceil(duration / 15) : 1;
                        const canStart = idx + blockCount <= generateTimeSlots.length;
                        const isActive = t === selectedTime || inBlock;
                        return (
                          <button
                            key={t}
                            onClick={() => canStart && setSelectedTime(t)}
                            disabled={!canStart}
                            className={isActive ? `${styles.timeSlotBtn} ${styles.timeSlotBtnActive}` : styles.timeSlotBtn}
                          >
                            {t}
                          </button>
                        );
                      })}
                    </div>

                    {/* Selected summary removed: use sidebar summary only */}
                  </div>
                ) : null}
              </div>
            ) : null}
          </div>
        </div>

        <div>
          <div className={styles.summaryWrapper}>
            <div className={styles.sectionTitle}>Appointment Summary</div>
            <div className={styles.sectionSubtitle}>Review your booking</div>

            <div style={{ marginTop: 12 }} className={styles.summaryCard}>
              <div className={styles.fieldLabel}>Doctor</div>
              <div className={styles.readField}>{selection.doctor?.name}</div>
            </div>

            <div className={styles.summaryCard}>
              <div className={styles.fieldLabel}>Facility</div>
              <div className={styles.readField}>{selection.facility?.name}</div>
            </div>

            <div className={styles.summaryCard}>
              <div className={styles.fieldLabel}>Date</div>
              <div className={styles.readField}>{date ? new Date(date).toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) : "No date selected"}</div>
            </div>

            <div className={styles.summaryCard}>
              <div className={styles.fieldLabel}>Time</div>
              <div className={styles.readField}>{(() => {
                const r = computeRange(selectedTime, duration);
                return r ? `${r.startLabel} - ${r.endLabel}` : 'No time selected';
              })()}</div>
              <div style={{ marginTop: 8, color: '#6b7280' }}>Duration: {duration ? `${duration} minutes` : '—'}</div>
            </div>

            <div style={{ marginTop: 8 }}>
              <button
                className={styles.continueBtn}
                disabled={!selectedTime}
                onClick={() => {
                  // persist the full booking selection and navigate to confirmation
                  try {
                    const payload = {
                      ...selection,
                      date,
                      time: selectedTime,
                      duration,
                    };
                    sessionStorage.setItem('bookingSelection', JSON.stringify(payload));
                  } catch {}
                  // navigate to confirmation tab
                  router.push('/protected/patient?tab=confirmation');
                }}
              >
                Continue to Confirmation
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
