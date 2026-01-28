export type Doctor = {
  id: string;
  name: string;
  email: string;
  gender: string;
  specialties: string[];
  states: string[];
};

export type Room = {
  id: string;
  name: string;
  specialty: string;
};

export type Facility = {
  id: string;
  name: string;
  city: string;
  state: string;
  address?: string;
  rooms: Room[];
};

export type State = {
  code: string;
  name: string;
};

export type Speciality = {
  code: string;
  name: string;
};

export type DoctorCreateRequest = {
  name: string;
  email: string;
  gender: string;
  password: string;
  stateCodes: string[];
  specialityCodes: string[];
};