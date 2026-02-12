"use client";

import styles from "../../doctor.module.scss";
import { useState, useEffect } from "react";
import DatePickerGrid from "./DatePickerGrid";
import FacilitySelector from "./FacilitySelector";
import TimeRangePicker from "./TimeRangePicker";
import ViewAvailabilityModal from "./ViewAvailabilityModal";
import { apiFetch, showInlineToast } from '@/lib/api';

type AvailabilityResp = {
  id: string;
  workDate: string;
  startTime: string;
  endTime: string;
  allowWalkIn?: boolean;
  bookable?: boolean;
  isBookable?: boolean;
  facilityId?: string;
  roomsTotal?: number;
  occupiedRooms?: number;
  availableRooms?: number;
  facilityName?: string; // Added facilityName
  facilityAddress?: string; // Added facilityAddress
  facilityState?: string; // Added facilityState
  roomAssignedId?: string;
  roomAssignmentStatus?: string;
};

type SavedItem = {
  id: string;
  date: string;
  facilityId: string | null;
  facilityName: string;
  start: string;
  end: string;
  hours: number;
  allowWalkIn?: boolean;
  isBookable?: boolean;
  specialities?: string[];
  roomsCount?: number;
  facilityAddress?: string;
  facilityState?: string;
  assigned?: boolean;
};
type FacilityApi = { id: string; name: string; city?: string; state?: string; rooms?: Array<Record<string, unknown>>; address?: string; specialityCodes?: string[] };
import AvailabilitySummary from "./AvailabilitySummary";

export default function AvailabilityView() {
  // require explicit user selection for date/start/end so Add Availability stays disabled until all fields are chosen
  const [selectedDate, setSelectedDate] = useState<string>("");
  const [selectedFacility, setSelectedFacility] = useState<string | null>(null);
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [weekIndex, setWeekIndex] = useState<number>(1);

  const parseMinutes = (t: string) => {
    const [h, m] = t.split(':').map((x) => parseInt(x, 10));
    return (isNaN(h) ? 0 : h) * 60 + (isNaN(m) ? 0 : m);
  };

  const totalMinutes = Math.max(0, parseMinutes(endTime) - parseMinutes(startTime));
  const totalHours = +(totalMinutes / 60).toFixed(2);
  const minHoursSatisfied = totalHours >= 4;
  const canAdd = !!selectedDate && !!selectedFacility && !!startTime && !!endTime && minHoursSatisfied;

  const [facilities, setFacilities] = useState<Array<{ id: string; name: string; city?: string; state?: string; rooms?: number; address?: string; roomsArray?: Array<Record<string, unknown>>; specialities?: string[]; specialityCodes?: string[] }>>([]);

  useEffect(() => {
    (async () => {
      try {
        const raw = localStorage.getItem('doctorProfile');
        if (!raw) return; // not logged in
        const doc = JSON.parse(raw);
        const doctorId = doc.id;
        const token = localStorage.getItem('doctorToken') ?? undefined;
        const resp = await apiFetch(`/api/doctors/${doctorId}/facilities`, token);
        if (Array.isArray(resp)) {
          setFacilities((resp as FacilityApi[]).map((f) => {
            const roomsArr = (f.rooms || []) as Array<Record<string, unknown>>;
            // prefer human-friendly 'specialty' from backend RoomResponse, fall back to common keys
            const specs = Array.from(new Set(roomsArr.map(r => (r['specialty'] ?? r['speciality'] ?? r['specialityCode'] ?? r['speciality_code'] ?? '').toString()).filter(s => s)));
        const specCodes = Array.from(new Set(roomsArr.map(r => (r['specialityCode'] ?? r['speciality'] ?? '').toString()).filter(s => s)));
        return { id: f.id, name: f.name, city: f.city, state: f.state ?? '', rooms: (roomsArr || []).length, address: f.address, roomsArray: roomsArr, specialities: specs, specialityCodes: specCodes };
          }));
        }
      } catch (err) {
        console.error('Failed to load facilities for doctor', err);
      }
    })();
  }, []);

  // load saved availabilities for this doctor so they persist across refresh
  useEffect(() => {
    (async () => {
      try {
        const raw = localStorage.getItem('doctorProfile');
        if (!raw) return;
        const doc = JSON.parse(raw);
        const doctorId = doc.id;
        const token = localStorage.getItem('doctorToken') ?? undefined;
        // Initial load: fetch saved availabilities, then for each unique date call the locked-availabilities
        // endpoint to ensure server-side locks are applied (which will deactivate other availabilities).
        // After that, re-fetch the availability list so the UI shows the updated active availabilities.
        const initialResp: AvailabilityResp[] = await apiFetch(`/api/doctors/${doctorId}/availability`, token);
        let mapped: SavedItem[] = [];
        if (Array.isArray(initialResp)) {
          mapped = initialResp.map(r => {
            const facility = r.facilityId ? facilities.find(f => f.id === r.facilityId) : undefined;
            return {
              id: r.id,
              date: r.workDate,
              facilityId: r.facilityId ?? null,
              facilityName: r.facilityName ?? facility?.name ?? 'Unknown',
              start: r.startTime,
              end: r.endTime,
              hours: Math.max(0, (parseMinutes(r.endTime) - parseMinutes(r.startTime)) / 60),
              specialities: [],
              roomsCount: typeof r.availableRooms === 'number' ? r.availableRooms : undefined,
              facilityAddress: r.facilityAddress ?? facility?.address,
              facilityState: r.facilityState ?? facility?.state,
              allowWalkIn: r.allowWalkIn ?? false,
              isBookable: r.bookable ?? (r.isBookable ?? true),
              assigned: Boolean(r.roomAssignedId) || (r.roomAssignmentStatus === 'ASSIGNED'),
            };
          });
        }

        // Ensure server enforces locks for the dates present in the initial response.
        try {
          const uniqueDates = Array.from(new Set(mapped.map(m => m.date))).filter(Boolean);
          await Promise.all(uniqueDates.map(d => apiFetch(`/api/doctors/${doctorId}/locked-availabilities?workDate=${d}`, token).catch(e => { console.warn('locked-availabilities check failed for', d, e); })));
          // re-fetch availabilities after locks may have been applied
          const refreshed: AvailabilityResp[] = await apiFetch(`/api/doctors/${doctorId}/availability`, token);
          if (Array.isArray(refreshed)) {
            const refreshedMapped: SavedItem[] = refreshed.map(r => {
              const facility = r.facilityId ? facilities.find(f => f.id === r.facilityId) : undefined;
              return {
                id: r.id,
                date: r.workDate,
                facilityId: r.facilityId ?? null,
                facilityName: r.facilityName ?? facility?.name ?? 'Unknown',
                start: r.startTime,
                end: r.endTime,
                hours: Math.max(0, (parseMinutes(r.endTime) - parseMinutes(r.startTime)) / 60),
                specialities: [],
                roomsCount: typeof r.availableRooms === 'number' ? r.availableRooms : undefined,
                facilityAddress: r.facilityAddress ?? facility?.address,
                facilityState: r.facilityState ?? facility?.state,
                allowWalkIn: r.allowWalkIn ?? false,
                isBookable: r.bookable ?? (r.isBookable ?? true),
                assigned: Boolean(r.roomAssignedId) || (r.roomAssignmentStatus === 'ASSIGNED'),
              };
            });
            setSaved(refreshedMapped);
          } else {
            setSaved(mapped);
          }
        } catch (e) {
          console.warn('Failed to refresh availabilities after lock checks', e);
          setSaved(mapped);
        }
      } catch (err) {
        console.error('Failed to load saved availabilities', err);
      }
    })();
  }, [facilities]);

  // pending (unsaved) availabilities shown in the summary
  const [pending, setPending] = useState<Array<{ id: string; date: string; facilityId: string | null; facilityName: string; start: string; end: string; hours: number; specialities?: string[]; roomsCount?: number; specialityCode?: string; facilityAddress?: string; facilityState?: string }>>([]);
  // saved availabilities (persisted) — local state until backend wiring
  const [saved, setSaved] = useState<SavedItem[]>([]);
  const [showSavedModal, setShowSavedModal] = useState(false);
  
  const handleSaveSchedule = () => {
    // call backend to persist pending items
    (async () => {
      try {
        const raw = localStorage.getItem('doctorProfile');
        if (!raw) throw new Error('Not authenticated as doctor');
        const doc = JSON.parse(raw);
        const doctorId = doc.id;
        const token = localStorage.getItem('doctorToken') ?? undefined;

        const payload = pending.map(p => ({
          workDate: p.date,
          startTime: p.start,
          endTime: p.end,
          specialityCode: p.specialityCode ?? undefined,
          facilityId: p.facilityId
        }));

        // Post the new availabilities
        const resp: AvailabilityResp[] = await apiFetch(`/api/doctors/${doctorId}/availability`, token, { method: 'POST', body: JSON.stringify(payload) });
        
        // After successful save, refetch all availabilities to get accurate merged data
        const allAvails: AvailabilityResp[] = await apiFetch(`/api/doctors/${doctorId}/availability`, token);
        
        if (Array.isArray(allAvails)) {
          const savedItems: SavedItem[] = allAvails.map((r: AvailabilityResp) => {
            const facility = r.facilityId ? facilities.find(f => f.id === r.facilityId) : undefined;
            const startMin = parseMinutes(r.startTime);
            const endMin = parseMinutes(r.endTime);
            const hours = +(Math.max(0, (endMin - startMin) / 60).toFixed(2));
            
            return {
              id: r.id,
              date: r.workDate,
              facilityId: r.facilityId ?? null,
              facilityName: r.facilityName ?? facility?.name ?? 'Unknown',
              start: r.startTime,
              end: r.endTime,
              hours: hours,
              specialities: [],
              roomsCount: typeof r.availableRooms === 'number' ? r.availableRooms : undefined,
              facilityAddress: r.facilityAddress ?? facility?.address,
              facilityState: r.facilityState ?? facility?.state,
              allowWalkIn: r.allowWalkIn ?? false,
              isBookable: r.bookable ?? (r.isBookable ?? true),
              assigned: Boolean(r.roomAssignedId) || (r.roomAssignmentStatus === 'ASSIGNED'),
            };
          });
          
          setSaved(savedItems);
          setPending([]);
          setShowSavedModal(true);
        }
      } catch (err) {
        console.error('Failed to save schedule', err);
        const msg = (err as Error).message || String(err);
        const lower = msg.toLowerCase();
        // Prefer showing a duplicate-specific toast when the message mentions duplication
        if (/duplicate|already exists|already added|conflict/i.test(lower) || /API\s409/i.test(msg)) {
          showInlineToast('Availability already exists for the selected facility, date and time.');
        } else if (/API\s401|API\s403/i.test(msg) && !/duplicate|already exists|conflict/i.test(lower)) {
          // Only show auth message if it's not actually a duplicate conflict
          showInlineToast('Authentication or authorization error. Please log in.');
        } else {
          showInlineToast('Failed to save schedule: ' + (msg.replace(/^API \d+: ?/, '') || 'Unknown error'));
        }
      }
    })();
  };

  const handleRemovePending = (id: string) => setPending(prev => prev.filter(x => x.id !== id));

  // Handler for date selection: reset facility and times when user picks a different date.
  // Using an explicit handler avoids calling setState synchronously inside a useEffect,
  // which can trigger lint warnings about cascading updates.
  const handleSelectDate = (iso: string) => {
    setSelectedDate(iso);
    setSelectedFacility(null);
    // require user to explicitly pick start/end after changing date
    setStartTime("");
    setEndTime("");
  };

  return (
    <>
    <div className={styles.scheduleContainer} style={{ position: 'relative' }}>
      <h2>Set Your Availability</h2>
      <p className={styles.subtitle}>
        Choose date, facility, and working hours.
      </p>

      <section className={styles.section}>
        <h4>1. Select Date</h4>
        <DatePickerGrid
          selectedDate={selectedDate}
          setSelectedDate={handleSelectDate}
          weekIndex={weekIndex}
          setWeekIndex={setWeekIndex}
        />
      </section>

      <section className={styles.section}>
        <h4>2. Select Facility</h4>
        <FacilitySelector
          facilities={facilities}
          selectedFacility={selectedFacility}
          setSelectedFacility={setSelectedFacility}
        />
      </section>

      <section className={styles.section}>
        <h4>3. Select Working Hours (Minimum 4 hours)</h4>
        <TimeRangePicker
          startTime={startTime}
          endTime={endTime}
          setStartTime={setStartTime}
          setEndTime={setEndTime}
        />

        {/* total hours box removed per design */}
      </section>

      
        {(startTime && endTime && !minHoursSatisfied) && (
          <div className={styles.warningBox} role="alert">Minimum 4 hours required to add availability</div>
        )}

        <div className={styles.actionsRight}>
          <button
            className={styles.viewAppointmentsBtn}
            disabled={!canAdd}
            title={
              !selectedDate ? 'Select a date' :
              !selectedFacility ? 'Select a facility' :
              !startTime || !endTime ? 'Select start and end times' :
              !minHoursSatisfied ? 'Total shift must be at least 4 hours' : 'Add Availability'
            }
              onClick={async () => {
              if (!canAdd) return;
              // add to pending summary
              const facility = facilities.find(f => f.id === selectedFacility) ?? { id: selectedFacility ?? '0', name: 'Unknown Facility' } as (typeof facilities)[number];
              const id = `${selectedDate}-${selectedFacility}-${startTime}-${endTime}`;
                let roomsCount = facility.rooms ?? 0;
                try {
                  const raw = localStorage.getItem('doctorProfile');
                  if (raw) {
                    const doc = JSON.parse(raw);
                    const doctorId = doc.id;
                    const token = localStorage.getItem('doctorToken') ?? undefined;
                    const avail = await apiFetch(`/api/doctors/${doctorId}/facilities/${selectedFacility}/availability?workDate=${selectedDate}&start=${startTime}&end=${endTime}`, token);
                    if (avail && typeof avail.availableRooms === 'number') {
                      roomsCount = avail.availableRooms;
                    }
                  }
                } catch (err) {
                  console.warn('Failed to fetch facility availability, falling back to static room count', err);
                }

                setPending(prev => prev.some(p => p.id === id) ? prev : [...prev, { id, date: selectedDate, facilityId: selectedFacility, facilityName: facility.name, start: startTime, end: endTime, hours: +(totalHours), specialities: facility.specialities ?? [], roomsCount, specialityCode: facility.specialityCodes?.[0] ?? undefined, facilityAddress: facility.address ?? '', facilityState: facility.state ?? '' }]);
            }}
          >
            Add Availability
          </button>
        </div>
        {/* capsule button inside the container to view saved availabilities */}
        <div style={{ position: 'absolute', right: 20, top: 20 }}>
          <button className={styles.viewAppointmentsBtn} onClick={async () => {
            try {
              const raw = localStorage.getItem('doctorProfile');
              if (!raw) throw new Error('Not authenticated as doctor');
              const doc = JSON.parse(raw);
              const doctorId = doc.id;
              const token = localStorage.getItem('doctorToken') ?? undefined;
              let resp: AvailabilityResp[] = [];
              // If a specific date is selected, call the locked-availabilities endpoint
              if (selectedDate && selectedDate.length > 0) {
                resp = await apiFetch(`/api/doctors/${doctorId}/locked-availabilities?workDate=${selectedDate}`, token);
              } else {
                resp = await apiFetch(`/api/doctors/${doctorId}/availability`, token);
              }
              if (Array.isArray(resp)) {
                const mapped: SavedItem[] = resp.map(r => {
                  const facility = r.facilityId ? facilities.find(f => f.id === r.facilityId) : undefined;
                  return {
                    id: r.id,
                    date: r.workDate,
                    facilityId: r.facilityId ?? null,
                    facilityName: r.facilityName ?? facility?.name ?? 'Unknown',
                    start: r.startTime,
                    end: r.endTime,
                    hours: Math.max(0, (parseMinutes(r.endTime) - parseMinutes(r.startTime)) / 60),
                    specialities: [],
                    roomsCount: typeof r.availableRooms === 'number' ? r.availableRooms : undefined,
                    facilityAddress: r.facilityAddress ?? facility?.address,
                    facilityState: r.facilityState ?? facility?.state,
                    allowWalkIn: r.allowWalkIn ?? false,
                    isBookable: r.bookable ?? (r.isBookable ?? true),
                    assigned: Boolean(r.roomAssignedId) || (r.roomAssignmentStatus === 'ASSIGNED'),
                  };
                });
                setSaved(mapped);
              }
            } catch (err) {
              console.error('Failed to load saved availabilities', err);
            }
            setShowSavedModal(true);
          }}>View availabilities</button>
        </div>

  <ViewAvailabilityModal
    open={showSavedModal}
    onClose={() => setShowSavedModal(false)}
    items={saved}
    onRemove={async (id) => {
      try {
        const raw = localStorage.getItem('doctorProfile');
        if (!raw) throw new Error('Not authenticated as doctor');
        const doc = JSON.parse(raw);
        const doctorId = doc.id;
        const token = localStorage.getItem('doctorToken') ?? undefined;
        // call backend delete endpoint (as requested path)
        await apiFetch(`/api/doctors/${doctorId}/availability/${id}/delete-availability`, token, { method: 'DELETE' });
        // remove locally
        setSaved(prev => prev.filter(x => x.id !== id));
      } catch (err) {
        console.error('Failed to delete availability', err);
        alert('Failed to delete availability: ' + (err as Error).message);
      }
    }}
  />
    </div>

    {/* Availability summary rendered in a separate container below the Set Availability card */}
    <AvailabilitySummary
      pending={pending}
      saved={saved}
      onSaveSchedule={handleSaveSchedule}
      onRemovePending={handleRemovePending}
      onViewSaved={() => setShowSavedModal(true)}
      showSavedInline={false}
    />
  </>
  );
}
