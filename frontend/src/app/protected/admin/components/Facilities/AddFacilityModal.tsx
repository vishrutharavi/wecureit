"use client";

import styles from "../../admin.module.scss";
import { useState, useEffect } from "react";
import { useDoctorMeta } from "../Doctors/useDoctors";
import { createFacility, createRoom, updateRoom, deactivateRoom, getFacilities } from "@/lib/admin/adminApi";
import { auth } from "@/lib/firebase";
import { Trash2 } from "lucide-react";
import type { Facility } from "../../types";

export default function AddFacilityModal({ onClose, initialFacility, onSaved }: { onClose: () => void; initialFacility?: Facility; onSaved?: (f: Facility) => void }) {
  const { states, specialities } = useDoctorMeta();
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [city, setCity] = useState("");
  const [zipCode, setZipCode] = useState("");
  const [stateCode, setStateCode] = useState("");
  const [loading, setLoading] = useState(false);

  // helper to retry a failing API call once by forcing a token refresh when we see a 403
  async function withTokenRetry<T>(token: string | null, fn: (t: string) => Promise<T>): Promise<T> {
    if (!token) throw new Error('No auth token');
    try {
      return await fn(token);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      if (msg.includes('API 403')) {
        const user = auth.currentUser;
        if (!user) throw err;
        const newToken = await user.getIdToken(true);
        return await fn(newToken);
      }
      throw err;
    }
  }

  // rooms: UI supports multiple room blocks
  const [rooms, setRooms] = useState<Array<{ id: number; serverId?: string; roomNumber?: string; extraSpecialityCode?: string | null }>>([
    { id: 1, roomNumber: '1', extraSpecialityCode: null },
  ]);

  const [initialRoomServerIds, setInitialRoomServerIds] = useState<string[]>([]);

  // when editing, populate initial values
  useEffect(() => {
    if (!initialFacility) return;
    setName(initialFacility.name ?? "");
    setAddress(initialFacility.address ?? "");
    setCity(initialFacility.city ?? "");
    // facility may not include zip in the response, leave blank if missing
    const facilityRecord = initialFacility as unknown as Record<string, unknown>;
    setZipCode(typeof facilityRecord.zipCode === 'string' ? (facilityRecord.zipCode as string) : "");
    setStateCode(initialFacility.state ?? "");
    // populate rooms from initialFacility if present (response room shape may vary)
    if (initialFacility.rooms && initialFacility.rooms.length) {
      const roomRecords = initialFacility.rooms as unknown as Record<string, unknown>[];
      const mapped = roomRecords.map((r, idx) => {
        const specialityVal = typeof r['specialityCode'] === 'string' ? (r['specialityCode'] as string) : (typeof r['speciality'] === 'string' ? (r['speciality'] as string) : null);
        const serverId = r['id'] ? String(r['id']) : undefined;
        const roomNumber = r['roomNumber'] ? String(r['roomNumber']) : String(idx + 1);
        return {
          id: idx + 1,
          serverId,
          roomNumber,
          extraSpecialityCode: specialityVal,
        };
      });
      setRooms(mapped);
      setInitialRoomServerIds(mapped.map((m) => m.serverId).filter(Boolean) as string[]);
    }
  }, [initialFacility]);

  function addRoom() {
    setRooms((prev) => {
      const nextId = prev.length ? Math.max(...prev.map((r) => r.id)) + 1 : 1;
      return [...prev, { id: nextId, roomNumber: String(nextId), extraSpecialityCode: null }];
    });
  }

  function removeRoom(id: number) {
    setRooms((prev) => prev.filter((r) => r.id !== id));
  }

  return (
    <div className={styles['modal-overlay']}>
      <div className={styles['modal-container']}>
        <div className={styles['modal-header']}>
          <h2 className={styles['modal-title']}>{initialFacility ? 'Edit Facility' : 'Add New Facility'}</h2>
          <button onClick={onClose} aria-label={initialFacility ? 'Close edit facility modal' : 'Close add facility modal'}>✕</button>
        </div>

        <div className={styles['facility-info']}>
          {initialFacility ? (
            <>Editing facility — changes will be saved to the selected facility.</>
          ) : (
            <>All rooms support <b>General Practice</b> by default.</>
          )}
        </div>

        <div className={styles['modal-body']}>
        <div className={styles['modal-section']}>
          <h4>Facility Info</h4>

          {/* 1st row: facility name and address side-by-side */}
          <div className={styles['modal-row-grid']} style={{ marginBottom: 12 }}>
            <input
              className={styles['modal-input']}
              placeholder="Facility Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={!!initialFacility}
            />

            <input
              className={styles['modal-input']}
              placeholder="Address"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              disabled={!!initialFacility}
            />
          </div>

          <div className={styles['modal-row-grid']} style={{ marginBottom: 12 }}>
            <input
              className={styles['modal-input']}
              placeholder="City"
              value={city}
              onChange={(e) => setCity(e.target.value)}
              disabled={!!initialFacility}
            />

            <input
              className={styles['modal-input']}
              placeholder="Zip Code"
              value={zipCode}
              // restrict to digits and max length 5
              inputMode="numeric"
              pattern="[0-9]*"
              maxLength={5}
              onChange={(e) => setZipCode(e.target.value.replace(/\D/g, '').slice(0, 5))}
              disabled={!!initialFacility}
            />
          </div>

          {/* 2nd row: state dropdown full width */}
          <div style={{ marginBottom: 12 }}>
            <select
              className={styles['state-select']}
              value={stateCode}
              onChange={(e) => setStateCode(e.target.value)}
              style={{ width: '100%' }}
              required
              disabled={!!initialFacility}
            >
              <option value="">Select state</option>
              {states.map((s) => (
                <option key={s.code} value={s.code}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
          <h4 style={{ margin: 0 }}>Rooms</h4>
          <button className={styles.viewAppointmentsBtn} onClick={addRoom}>+ Add Room</button>
        </div>

  {rooms.map((room) => (
          <div key={room.id} className={styles['facility-room']}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h4 style={{ margin: 0 }}>{`Room ${room.roomNumber ?? room.id}`}</h4>
              {rooms.length > 1 && (
                <button
                  type="button"
                  onClick={() => removeRoom(room.id)}
                  aria-label="Delete room"
                  style={{
                    background: 'transparent',
                    border: 'none',
                    cursor: 'pointer',
                    padding: 6,
                    display: 'flex',
                    alignItems: 'center',
                  }}
                >
                  <Trash2 color="#ef4444" size={16} />
                </button>
              )}
            </div>

            {/* no room number display necessary here (room label already shows the number) */}

            <div className={styles['facility-specialtyGrid']}>
            {/* Always show General Practice as selected and disabled */}
            <label className={styles['facility-checkbox']}>
              <input type="checkbox" checked={true} disabled /> General Practice
            </label>

            {(specialities?.length ? specialities : [])
              .filter((s) => s.name.toLowerCase() !== "general practice")
              .map((spec) => {
                const selected = room.extraSpecialityCode === spec.code;
                const anySelected = !!room.extraSpecialityCode;

                return (
                  <label key={spec.code} className={styles['facility-checkbox']}>
                    <input
                      type="checkbox"
                      checked={selected}
                      // when one extra is selected, disable other extras
                      disabled={anySelected && !selected}
                      onChange={(e) => {
                        const checked = e.target.checked;
                        setRooms((prev) => prev.map((r) => (r.id === room.id ? { ...r, extraSpecialityCode: checked ? spec.code : null } : r)));
                      }}
                    />
                    {spec.name}
                  </label>
                );
              })}
          </div>
          </div>
        ))}

        </div>

        <div className={styles['modal-footer']}>
          <button className={styles.secondaryBtn} onClick={onClose} disabled={loading}>Cancel</button>
          <button
            className={styles.viewAppointmentsBtn}
            disabled={loading}
            onClick={async () => {
                // validation for create mode only: require all facility-level fields
                if (!initialFacility) {
                  const n = name.trim();
                  const a = address.trim();
                  const c = city.trim();
                  const s = stateCode.trim();
                  const z = zipCode.trim();

                  if (!n || !a || !c || !s || !z) {
                    alert('Please fill all facility fields: Name, Address, City, State and Zip Code');
                    return;
                  }

                  // zip code must be exactly 5 digits
                  if (!/^\d{5}$/.test(z)) {
                    alert('Zip Code must be exactly 5 digits');
                    return;
                  }
                }

              setLoading(true);
              try {
                const user = auth.currentUser;
                // Force token refresh to pick up any custom role claims set server-side
                const token = user ? await user.getIdToken(true) : null;
                if (!token) throw new Error('No auth token');

                if (initialFacility && initialFacility.id) {
                  // edit mode: only sync rooms (facility-level fields are not editable in this modal)
                  const facilityId = String(initialFacility.id);

                  // sync rooms: update existing, create new, deactivate removed
                  // pick general practice code fallback
                  const gp = specialities?.find((s) => s.name.toLowerCase() === 'general practice');
                  for (const r of rooms) {
                    const specialityCode = r.extraSpecialityCode || (gp ? gp.code : (specialities && specialities.length ? specialities[0].code : null));
                    const payload = { roomNumber: r.roomNumber, specialityCode };

                    if (r.serverId) {
                      try {
                        await withTokenRetry(token, (t) => updateRoom(t, r.serverId!, { roomNumber: payload.roomNumber, specialityCode: payload.specialityCode ?? undefined }));
                      } catch (err) {
                        console.warn('Failed to update room', r.serverId, err);
                      }
                    } else {
                      // new room -> create
                      if (!payload.specialityCode) {
                        console.warn('Skipping create room: no speciality code for', r);
                      } else {
                        try {
                          await withTokenRetry(token, (t) => createRoom(t, facilityId, { roomNumber: r.roomNumber || String(r.id), specialityCode: payload.specialityCode as string }));
                        } catch (err) {
                          console.warn('Failed to create room', err);
                        }
                      }
                    }
                  }

                  // detect deletions
                  const currentServerIds = rooms.map((r) => r.serverId).filter(Boolean) as string[];
                  const removed = initialRoomServerIds.filter((id) => !currentServerIds.includes(id));
                  for (const rid of removed) {
                    try {
                      await withTokenRetry(token, (t) => deactivateRoom(t, rid));
                    } catch (err) {
                      console.warn('Failed to deactivate room', rid, err);
                    }
                  }

                  // refetch facility list and provide updated facility to parent
                  try {
                    const all = (await withTokenRetry(token, (t) => getFacilities(t))) as Facility[];
                    const updatedFacility = all.find((f) => String(f.id) === String(initialFacility.id));
                    alert('Facility updated');
                    if (onSaved && updatedFacility) onSaved(updatedFacility);
                        try { window.dispatchEvent(new CustomEvent('wecureit:facilities-changed', { detail: updatedFacility })); } catch {}
                  } catch (err) {
                    console.warn('Failed to reload facilities after update', err);
                    alert('Facility updated (could not refresh list)');
                  }

                  onClose();
                  return;
                }

                // create facility
                // before creating, fetch existing facilities and validate duplicates based on address+state+zip (ignore whitespace)
                const normalize = (s?: string) => (s || '').replace(/\s+/g, '').toLowerCase();
                try {
                  const existing = (await withTokenRetry(token, (t) => getFacilities(t))) as Facility[];
                  const addrNorm = normalize(address.trim());
                  const zipNorm = normalize(zipCode.trim());
                  const stateNorm = normalize(stateCode.trim());

                  const duplicate = existing.some((f: Facility) => {
                    const rec = f as unknown as Record<string, unknown>;
                    const fa = normalize(String(rec.address ?? ''));
                    const fz = normalize(String(rec.zipCode ?? rec.zip ?? ''));
                    const fs = normalize(String(rec.state ?? ''));
                    return fa === addrNorm && fz === zipNorm && fs === stateNorm;
                  });

                  if (duplicate) {
                    alert('A facility with the same address, state and ZIP already exists. Please check the list before adding.');
                    setLoading(false);
                    return;
                  }
                } catch (err) {
                  console.warn('Failed to validate duplicates before create, proceeding with create', err);
                }

                const facilityPayload = { name, address, city, stateCode, zipCode };
                const created = await createFacility(token, facilityPayload);
                const facilityId = created?.id;
                if (!facilityId) throw new Error('Failed to create facility');

                // create rooms (one request per room)
                for (const r of rooms) {
                  let specialityCode = r.extraSpecialityCode || null;
                  if (!specialityCode) {
                    const gp = specialities?.find((s) => s.name.toLowerCase() === 'general practice');
                    if (gp) specialityCode = gp.code;
                    else if (specialities && specialities.length) specialityCode = specialities[0].code;
                  }

                  if (!specialityCode) {
                    console.warn('Skipping room creation: no speciality code available for', r);
                    continue;
                  }

                  await createRoom(token, facilityId, {
                    roomNumber: r.roomNumber || String(r.id),
                    specialityCode,
                  });
                }

                // signal other UI (facility grid) to reload so new facility appears immediately
                try { window.dispatchEvent(new CustomEvent('wecureit:facilities-changed', { detail: created })); } catch {}
                alert('Facility and room(s) created');
                onClose();
              } catch (err) {
                console.error(err);
                const msg = err instanceof Error ? err.message : String(err);
                alert('Failed to create facility: ' + msg);
              } finally {
                setLoading(false);
              }
            }}
          >
            {loading ? (initialFacility ? 'Saving...' : 'Creating...') : (initialFacility ? 'Save Changes' : 'Create Facility')}
          </button>
        </div>
      </div>
    </div>
  );
}
