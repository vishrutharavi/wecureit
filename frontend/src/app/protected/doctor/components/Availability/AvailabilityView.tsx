"use client";

import styles from "../../doctor.module.scss";
import { useState } from "react";
import DatePickerGrid from "./DatePickerGrid";
import FacilitySelector from "./FacilitySelector";
import TimeRangePicker from "./TimeRangePicker";
import ViewAvailabilityModal from "./ViewAvailabilityModal";
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

  const facilities = [
    { id: "1", name: "Downtown Medical Center", city: "Washington DC", rooms: 12 },
    { id: "2", name: "Alexandria Main Hospital", city: "Alexandria", rooms: 15 },
    { id: "3", name: "Bethesda Health Center", city: "Bethesda", rooms: 9 },
  ];

  // pending (unsaved) availabilities shown in the summary
  const [pending, setPending] = useState<Array<{ id: string; date: string; facilityId: string | null; facilityName: string; start: string; end: string; hours: number }>>([]);
  // saved availabilities (persisted) — local state until backend wiring
  const [saved, setSaved] = useState<typeof pending>([]);
  const [showSavedModal, setShowSavedModal] = useState(false);
  const [hideSavedInline, setHideSavedInline] = useState(false);
  
  const handleSaveSchedule = () => {
    setSaved((s) => [...s, ...pending]);
    setPending([]);
    setShowSavedModal(true);
    // hide the inline saved availabilities container after saving (we show modal instead)
    setHideSavedInline(true);
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
            onClick={() => {
              if (!canAdd) return;
              // add to pending summary
              const facility = facilities.find(f => f.id === selectedFacility) ?? { id: selectedFacility ?? '0', name: 'Unknown Facility' };
              const id = `${selectedDate}-${selectedFacility}-${startTime}-${endTime}`;
              setPending(prev => prev.some(p => p.id === id) ? prev : [...prev, { id, date: selectedDate, facilityId: selectedFacility, facilityName: facility.name, start: startTime, end: endTime, hours: +(totalHours) }]);
            }}
          >
            Add Availability
          </button>
        </div>
        {/* capsule button inside the container to view saved availabilities */}
        <div style={{ position: 'absolute', right: 20, top: 20 }}>
          <button className={styles.viewAppointmentsBtn} onClick={() => setShowSavedModal(true)}>View availabilities</button>
        </div>

  <ViewAvailabilityModal open={showSavedModal} onClose={() => setShowSavedModal(false)} items={saved} onRemove={(id) => setSaved(prev => prev.filter(x => x.id !== id))} />
    </div>

    {/* Availability summary rendered in a separate container below the Set Availability card */}
    <AvailabilitySummary
      pending={pending}
      saved={saved}
      onSaveSchedule={handleSaveSchedule}
      onRemovePending={handleRemovePending}
      onViewSaved={() => setShowSavedModal(true)}
      showSavedInline={!hideSavedInline}
    />
  </>
  );
}
