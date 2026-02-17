"use client";

import React from "react";
import { apiFetch, showInlineToast } from '@/lib/api';
import { useRouter } from "next/navigation";
import { toLocalIso } from '@/lib/dateUtils';
import styles from "../../patient.module.scss";
import DatePickerGrid from "./DatePickerGrid";

type Selection = {
  doctor?: { id: string; name: string } | null;
  facility?: { id: string; name: string } | null;
  specialty?: { id: string; name: string } | null;
};

type SlotSuggestion = {
  doctorId: string;
  doctorName: string;
  specialty: string;
  specialtyId: string;
  facilityId: string;
  facilityName: string;
  date: string;
  startTime: string;
  endTime: string;
  durationMinutes: number;
  reason: string;
  doctorAvailabilityId: string;
};

// saved card shape used by the payment UI
// SavedCard type removed — payment UI omitted from appointment summary

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
  // optional id returned by backend
  id?: string;
  availabilityId?: string;
  occupiedAppointments?: string[];
};

export default function DateAndTimeSelection() {
  const router = useRouter();
  const [selection, setSelection] = React.useState<Selection | null>(null);
  const [availableDates, setAvailableDates] = React.useState<string[] | null>(null);
  const [availabilities, setAvailabilities] = React.useState<AvailabilityResp[] | null>(null);
  type ServerSlot = { startAt: string; endAt: string; status: string; availabilityId?: string | null };
  const [bookingSlots, setBookingSlots] = React.useState<Array<ServerSlot> | null>(null);
  // date is null until user explicitly picks one from the mini calendar
  const [date, setDate] = React.useState<string | null>(null);
  const [duration, setDuration] = React.useState<number | null>(null);
  const [selectedTime, setSelectedTime] = React.useState<string | null>(null);
  const [selectedIndex, setSelectedIndex] = React.useState<number | null>(null);
  const [selectedDoctorAvailabilityId, setSelectedDoctorAvailabilityId] = React.useState<string | null>(null);
  const [chiefComplaints, setChiefComplaints] = React.useState<string>("");
  const [chiefError, setChiefError] = React.useState<string | null>(null);
  const [suggestedSlots, setSuggestedSlots] = React.useState<SlotSuggestion[] | null>(null);
  const [quickBookSlot, setQuickBookSlot] = React.useState<SlotSuggestion | null>(null);
  const [quickBookComplaints, setQuickBookComplaints] = React.useState<string>("");
  const [quickBookError, setQuickBookError] = React.useState<string | null>(null);

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

  // parse ISO date YYYY-MM-DD as local Date (avoid UTC parsing)
  const parseLocalDate = (iso: string | null) => {
    if (!iso) return new Date();
    const m = iso.match(/^(\d{4})-(\d{2})-(\d{2})$/);
    if (!m) return new Date(iso);
    const y = parseInt(m[1], 10);
    const mo = parseInt(m[2], 10) - 1;
    const d = parseInt(m[3], 10);
    return new Date(y, mo, d);
  };

  React.useEffect(() => {
    try {
      const raw = sessionStorage.getItem("bookingSelection");
      if (raw) {
        const parsed = JSON.parse(raw);
        setSelection(parsed);
        if (parsed.chiefComplaints) setChiefComplaints(String(parsed.chiefComplaints));
        if (parsed.doctorAvailabilityId) setSelectedDoctorAvailabilityId(String(parsed.doctorAvailabilityId));
        // restore duration if it was previously selected, otherwise use 30 as default
        if (parsed.duration) {
          setDuration(parsed.duration);
        } else {
          setDuration(30);
        }
      } else {
        // If no booking selection exists, default to 30 minutes
        setDuration(30);
      }
    } catch {
      // On error, default to 30 minutes
      setDuration(30);
    }
  }, []);

  // clear any previously selected time when date changes to avoid stale selection
  React.useEffect(() => {
    setSelectedTime(null);
    setSelectedIndex(null);
    setSelectedDoctorAvailabilityId(null);
  }, [date]);

  // payment methods removed from UI — selection handled elsewhere if needed

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
        const fromIso = toLocalIso(from);
        const toIso = toLocalIso(to);
        const url = `/api/doctors/${doctorId}/availability?from=${fromIso}&to=${toIso}`;
        const resp = await apiFetch(url) as AvailabilityResp[];
        if (canceled) return;

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

        // If the patient selected a specific facility, ensure dates locked to a different facility
        // are not shown. For each date, call the locked-availabilities endpoint which will return
        // only the active availabilities for that date (and will deactivate other availabilities
        // server-side if a lock exists). If the returned list does not contain any availability for
        // the selected facility, we exclude that date from the UI.
        if (selection.facility?.id && dates.length > 0) {
          try {
            const facId = selection.facility.id;
            const checks = await Promise.all(dates.map(async (d) => {
              try {
                const resp = await apiFetch(`/api/doctors/${doctorId}/locked-availabilities?workDate=${d}`) as AvailabilityResp[];
                if (Array.isArray(resp) && resp.length > 0) {
                  // keep the date only if any returned availability belongs to the selected facility
                  return resp.some((a: AvailabilityResp) => String(a.facilityId || '').trim() === String(facId).trim());
                }
                // if server returned no availabilities, don't include the date for this facility
                return false;
              } catch (e) {
                // on error, be conservative and exclude the date so patients don't see bookings they can't make
                console.warn('locked-availabilities check failed for date', d, e);
                return false;
              }
            }));
            const filteredDates = dates.filter((_, idx) => checks[idx]);
            setAvailableDates(filteredDates);
            // also filter availabilities to entries matching remaining dates and facility
            setAvailabilities(filtered.filter((r) => filteredDates.includes(r.workDate)));
          } catch (e) {
            console.error('Failed to apply facility-lock filtering', e);
            // fallback to showing computed dates for now
            setAvailableDates(dates);
            setAvailabilities(filtered);
          }
        } else {
          setAvailableDates(dates);
          setAvailabilities(filtered);
        }
      } catch (err) {
        console.error('Failed to load availabilities', err);
        showInlineToast('Failed to load doctor availability');
        setAvailableDates(null);
        setAvailabilities(null);
      }
    })();
    return () => { canceled = true; };
  }, [selection]);

  // fetch optimal slot suggestions when selection and duration are ready
  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!selection || !selection.doctor?.id || !selection.facility?.id) {
        setSuggestedSlots(null);
        return;
      }

      try {
        const requestBody = {
          doctorId: selection.doctor.id,
          facilityId: selection.facility.id,
          specialtyCode: selection.specialty?.id || null,
          duration: duration || 30
        };

        const response = await apiFetch('/api/patients/booking/suggest-optimal-slots', undefined, {
          method: 'POST',
          body: JSON.stringify(requestBody)
        });

        if (cancelled) return;

        if (Array.isArray(response)) {
          setSuggestedSlots(response);
        } else {
          setSuggestedSlots(null);
        }
      } catch (err) {
        console.error('Failed to fetch optimal slot suggestions', err);
        setSuggestedSlots(null);
      }
    })();

    return () => { cancelled = true; };
  }, [selection, duration]);

  // fetch slot-level availability for selected date (15-min slots with status)
  React.useEffect(() => {
    let cancelled = false;
    (async () => {
      if (!selection || !selection.doctor?.id || !date) {
        setBookingSlots(null);
        return;
      }
      try {
        const doctorId = selection.doctor.id;
        const facilityId = selection.facility?.id;
        const params = new URLSearchParams();
        params.set('doctorId', doctorId);
        if (facilityId) params.set('facilityId', facilityId);
        params.set('date', date);
        if (duration) params.set('duration', String(duration));
        if (selection.specialty?.id) params.set('specialityCode', selection.specialty.id);
        const url = `/api/patients/booking/availability?${params.toString()}`;
        const resp = await apiFetch(url) as { slots?: ServerSlot[] } | null;
        if (cancelled) return;
        if (resp && Array.isArray(resp.slots)) {
          setBookingSlots(resp.slots.map((s) => ({ startAt: s.startAt, endAt: s.endAt, status: s.status, availabilityId: s.availabilityId || null })));
        } else {
          setBookingSlots(null);
        }
      } catch (err) {
        console.error('Failed to load booking slots', err);
        showInlineToast('Failed to load time slots');
        setBookingSlots(null);
      }
    })();
    return () => { cancelled = true; };
  }, [selection, date, duration]);

  // quick lookup of availabilities for the selected date (used to show message before duration)
  const generateTimeSlots = React.useMemo(() => {
    if (!date) return { labels: [] as string[], ids: [] as string[], disabled: [] as boolean[] };
    // today's date and current local minutes used to filter out past slots when showing today's availability
    const today = (() => {
      const d = new Date();
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    })();
    const nowMinutesLocal = (() => {
      const dn = new Date();
      return dn.getHours() * 60 + dn.getMinutes();
    })();
    // if server provided slot-level data, use it
      if (bookingSlots && bookingSlots.length > 0) {
      // build a sorted list by start minute
      const entries = bookingSlots.map(s => {
        const dt = new Date(s.startAt);
        const minute = dt.getHours() * 60 + dt.getMinutes();
        const label = dt.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
  return { minute, label, status: s.status, availabilityId: s.availabilityId || null };
      }).sort((a, b) => a.minute - b.minute);
      // if the selected date is today, filter out slots earlier than the current time
      if (date === today) {
        const filtered = entries.filter(e => e.minute >= nowMinutesLocal);
        const labels = filtered.map(e => e.label);
        const ids = filtered.map(e => e.availabilityId || '');
        const disabled = filtered.map(e => e.status !== 'AVAILABLE');
        return { labels, ids, disabled };
      }
      const labels = entries.map(e => e.label);
      const ids = entries.map(e => e.availabilityId || '');
      const disabled = entries.map(e => e.status !== 'AVAILABLE');
      return { labels, ids, disabled };
    }

    // fallback: build from availability windows
    const rows = (availabilities || []).filter((r) => r.workDate === date && (r.bookable || r.isBookable));
    const granularity = 15;
    const parseHM = (s: string) => {
      if (!s) return null;
      const m = s.match(/^(\d{1,2}):(\d{2})$/);
      if (!m) return null;
      const hh = parseInt(m[1], 10);
      const mm = parseInt(m[2], 10);
      return hh * 60 + mm;
    };
    const slotMap = new Map<number, { label: string; disabled: boolean }>();
    for (const r of rows) {
      const s = parseHM(r.startTime || '');
      const e = parseHM(r.endTime || '');
      if (s == null || e == null) continue;
      const lastStart = e - granularity;
      for (let t = s; t <= lastStart; t += granularity) {
        if (!slotMap.has(t)) {
          const label = (() => {
            const hh = Math.floor(t / 60);
            const mm = t % 60;
            const period = hh >= 12 ? 'PM' : 'AM';
            let hour = hh % 12; if (hour === 0) hour = 12;
            return `${hour}:${String(mm).padStart(2, '0')} ${period}`;
          })();
          slotMap.set(t, { label, disabled: false });
        }
      }
    }
    // (today/nowMinutesLocal are defined earlier for server branch)
    let sorted = Array.from(slotMap.keys()).sort((a, b) => a - b);
    if (date === today) sorted = sorted.filter(m => m >= nowMinutesLocal);
    const labels = sorted.map(m => slotMap.get(m)!.label);
    const ids = sorted.map(() => '');
    const disabled = sorted.map(m => slotMap.get(m)!.disabled);
    return { labels, ids, disabled };
  }, [date, bookingSlots, availabilities]);

  const rowsForDate = date ? (availabilities || []).filter((r) => r.workDate === date && (r.bookable || r.isBookable)) : [];

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

      {/* Optimal Slot Suggestions */}
      {suggestedSlots && suggestedSlots.length > 0 && (
        <div className={styles.suggestionsPanel}>
          <div className={styles.sectionTitle}>Recommended Time Slots</div>
          <div className={styles.sectionSubtitle}>
            These slots optimize schedule efficiency and minimize doctor idle time
          </div>

          <div className={styles.suggestionGrid}>
            {suggestedSlots.map((slot, idx) => (
              <div
                key={idx}
                className={styles.suggestionCard}
                onClick={() => {
                  setQuickBookSlot(slot);
                  setQuickBookComplaints(chiefComplaints);
                  setQuickBookError(null);
                }}
              >
                <div className={styles.suggestionBadge}>#{idx + 1}</div>
                <div className={styles.suggestionDate}>
                  {new Date(slot.date + 'T00:00:00').toLocaleDateString('en-US', {
                    weekday: 'short',
                    month: 'short',
                    day: 'numeric'
                  })}
                </div>
                <div className={styles.suggestionTime}>
                  {slot.startTime} - {slot.endTime}
                </div>
                <div className={styles.suggestionReason}>
                  {slot.reason}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

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
                {rowsForDate.length === 0 ? (
                  <div className={styles.emptyCard}>No available slots for this date</div>
                ) : (
                  <>
                    <div className={styles.durationGrid}>
                      {[15, 30, 60].map((d) => (
                        <button
                          key={d}
                          onClick={() => {
                            setDuration(d);
                            // clear any previously selected time when duration changes
                            setSelectedTime(null);
                            setSelectedIndex(null);
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
                            {generateTimeSlots.labels.length === 0 ? (
                            <div className={styles.emptyCard}>No available slots for this date</div>
                          ) : (
                            generateTimeSlots.labels.map((t, idx) => {
                              // compute whether this slot falls within the selected time block
                              let inBlock = false;
                              if (selectedIndex !== null && duration) {
                                const blockCount = Math.ceil(duration / 15);
                                if (selectedIndex >= 0 && idx >= selectedIndex && idx < selectedIndex + blockCount) inBlock = true;
                              }
                              // determine if this slot is a valid start (enough consecutive AVAILABLE slots for the duration)
                              const blockCount = duration ? Math.ceil(duration / 15) : 1;
                              const enoughSlots = idx + blockCount <= generateTimeSlots.labels.length;
                              const allAvailable = enoughSlots && Array.from({ length: blockCount }, (_, i) => idx + i).every(
                                i => !Array.isArray(generateTimeSlots.disabled) || !generateTimeSlots.disabled[i]
                              );
                              const canStart = enoughSlots && allAvailable;
                              const isActive = (selectedIndex !== null && idx === selectedIndex) || inBlock;
                              const availabilityId = generateTimeSlots.ids[idx] || null;
                              const slotDisabled = Array.isArray(generateTimeSlots.disabled) ? Boolean(generateTimeSlots.disabled[idx]) : false;
                              return (
                                <button
                                  key={`${t}-${idx}-${availabilityId || ''}`}
                                  onClick={() => {
                                    if (!canStart) return;
                                    if (slotDisabled) return;
                                    setSelectedTime(t);
                                    setSelectedIndex(idx);
                                    setSelectedDoctorAvailabilityId(availabilityId);
                                    // persist selection so the confirmation step has doctorAvailabilityId
                                    try {
                                      const raw = sessionStorage.getItem('bookingSelection');
                                      const bs = raw ? JSON.parse(raw) : {};
                                      bs.date = date;
                                      bs.time = t;
                                      bs.doctorAvailabilityId = availabilityId || null;
                                      bs.duration = duration;
                                      // keep chiefComplaints if already present
                                      sessionStorage.setItem('bookingSelection', JSON.stringify(bs));
                                    } catch {
                                      // ignore storage errors
                                    }
                                  }}
                                  disabled={!canStart || slotDisabled}
                                  className={isActive ? `${styles.timeSlotBtn} ${styles.timeSlotBtnActive}` : slotDisabled ? `${styles.timeSlotBtn} ${styles.timeSlotBtnDisabled ?? ''}` : styles.timeSlotBtn}
                                >
                                  {t}
                                </button>
                              );
                            })
                          )}
                        </div>

                        {/* Selected summary removed: use sidebar summary only */}
                      </div>
                    ) : null}
                  </>
                )}
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
              <div className={styles.fieldLabel}>Speciality</div>
              <div className={styles.readField}>{selection.specialty?.name ?? 'General'}</div>
            </div>

            <div className={styles.summaryCard}>
              <div className={styles.fieldLabel}>Date</div>
              <div className={styles.readField}>{date ? parseLocalDate(date).toLocaleDateString(undefined, { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' }) : "No date selected"}</div>
            </div>

            <div className={styles.summaryCard}>
              <div className={styles.fieldLabel}>Time</div>
              <div className={styles.readField}>{(() => {
                const r = computeRange(selectedTime, duration);
                return r ? `${r.startLabel} - ${r.endLabel}` : 'No time selected';
              })()}</div>
              <div style={{ marginTop: 8, color: '#6b7280' }}>Duration: {duration ? `${duration} minutes` : '—'}</div>
            </div>

            {/* Payment method removed from appointment summary per design */}

            <div style={{ marginTop: 8 }}>
              <div>
                <div className={styles.fieldLabel}>Chief Complaints <span style={{ color: '#c0392b' }}>*</span></div>
                <textarea
                  className={styles.inputField}
                  placeholder="Briefly describe the patient's chief complaints"
                  value={chiefComplaints}
                  onChange={(e) => {
                    setChiefComplaints(e.target.value);
                    if (chiefError) setChiefError(null);
                  }}
                  rows={3}
                />
                {chiefError ? <div className={styles.errorText}>{chiefError}</div> : null}
              </div>

              <div style={{ marginTop: 8 }}>
                <button
                  className={styles.continueBtn}
                  disabled={!selectedTime}
                  onClick={() => {
                    // validate chief complaints
                    if (!chiefComplaints || !chiefComplaints.trim()) {
                      setChiefError('Please enter the chief complaints to proceed');
                      return;
                    }

                    // persist the full booking selection and navigate to confirmation
                    try {
                      const payload = {
                        ...selection,
                        date,
                        time: selectedTime,
                        duration,
                        doctorAvailabilityId: selectedDoctorAvailabilityId,
                        chiefComplaints: chiefComplaints.trim(),
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

      {/* Quick-book modal for recommended slot cards */}
      {quickBookSlot && (
        <div className={styles['modal-overlay']} onClick={() => setQuickBookSlot(null)}>
          <div className={styles['modal-container']} onClick={(e) => e.stopPropagation()}>
            <div className={styles['modal-header']}>
              <h2 className={styles['modal-title']}>Book Recommended Slot</h2>
              <button onClick={() => setQuickBookSlot(null)} className={styles['modal-close'] ?? ''}>&#x2715;</button>
            </div>
            <div className={styles['modal-body']}>
              <div className={styles.quickBookDetails}>
                <div className={styles.quickBookRow}>
                  <span className={styles.quickBookLabel}>Doctor</span>
                  <span className={styles.quickBookValue}>{selection?.doctor?.name || quickBookSlot.doctorName}</span>
                </div>
                <div className={styles.quickBookRow}>
                  <span className={styles.quickBookLabel}>Facility</span>
                  <span className={styles.quickBookValue}>{selection?.facility?.name || quickBookSlot.facilityName}</span>
                </div>
                <div className={styles.quickBookRow}>
                  <span className={styles.quickBookLabel}>Specialty</span>
                  <span className={styles.quickBookValue}>{selection?.specialty?.name || quickBookSlot.specialty}</span>
                </div>
                <div className={styles.quickBookRow}>
                  <span className={styles.quickBookLabel}>Date</span>
                  <span className={styles.quickBookValue}>
                    {new Date(quickBookSlot.date + 'T00:00:00').toLocaleDateString('en-US', {
                      weekday: 'long', month: 'long', day: 'numeric', year: 'numeric'
                    })}
                  </span>
                </div>
                <div className={styles.quickBookRow}>
                  <span className={styles.quickBookLabel}>Time</span>
                  <span className={styles.quickBookValue}>{quickBookSlot.startTime} - {quickBookSlot.endTime}</span>
                </div>
                <div className={styles.quickBookRow}>
                  <span className={styles.quickBookLabel}>Duration</span>
                  <span className={styles.quickBookValue}>{quickBookSlot.durationMinutes} minutes</span>
                </div>
              </div>

              <div style={{ marginTop: 16 }}>
                <div className={styles.fieldLabel}>Chief Complaints <span style={{ color: '#c0392b' }}>*</span></div>
                <textarea
                  className={styles.inputField}
                  placeholder="Briefly describe the patient's chief complaints"
                  value={quickBookComplaints}
                  onChange={(e) => {
                    setQuickBookComplaints(e.target.value);
                    if (quickBookError) setQuickBookError(null);
                  }}
                  rows={3}
                />
                {quickBookError && <div className={styles.errorText}>{quickBookError}</div>}
              </div>

              <div style={{ marginTop: 16, display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
                <button
                  className={styles.viewAppointmentsBtn}
                  onClick={() => setQuickBookSlot(null)}
                >
                  Cancel
                </button>
                <button
                  className={styles.continueBtn}
                  onClick={() => {
                    if (!quickBookComplaints || !quickBookComplaints.trim()) {
                      setQuickBookError('Please enter the chief complaints to proceed');
                      return;
                    }
                    try {
                      // Convert 24-hour format time to 12-hour format with AM/PM
                      const convert24To12Hour = (time24: string) => {
                        const match = time24.match(/(\d{1,2}):(\d{2})/);
                        if (!match) return time24;
                        let hours = parseInt(match[1], 10);
                        const minutes = match[2];
                        const ampm = hours >= 12 ? 'PM' : 'AM';
                        hours = hours % 12 || 12;
                        return `${hours}:${minutes} ${ampm}`;
                      };
                      
                      const payload = {
                        ...selection,
                        date: quickBookSlot.date,
                        time: convert24To12Hour(quickBookSlot.startTime),
                        duration: quickBookSlot.durationMinutes,
                        doctorAvailabilityId: quickBookSlot.doctorAvailabilityId,
                        chiefComplaints: quickBookComplaints.trim(),
                      };
                      sessionStorage.setItem('bookingSelection', JSON.stringify(payload));
                    } catch {}
                    router.push('/protected/patient?tab=confirmation');
                  }}
                >
                  Continue to Confirmation
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
