export type Specialty = { id?: string; name: string };

export type Facility = { id: string; name: string; specialties?: Array<{ code?: string; name?: string }> };

export type Doctor = {
  id: string;
  name: string;
  specialties?: Array<{ code?: string; name?: string }>;
  facilities?: Array<{ id: string; name?: string }>;
};

export type BookingResponse = {
  specialties?: Array<{ code: string; name: string }>;
  facilities?: Array<{ id: string; name: string; specialties?: Array<{ code?: string; name?: string }> }>;
  doctors?: Array<{
    id: string;
    displayName?: string;
    name?: string;
    specialties?: Array<{ code?: string; name?: string }>;
    facilities?: Array<{ id: string; name?: string }>;
  }>;
};
