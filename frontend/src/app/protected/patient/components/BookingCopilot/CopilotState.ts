export type BookingIntent = {
  specialty: string | null;
  facilityName: string | null;
  doctorName: string | null;
  preferredDateStart: string | null;
  preferredDateEnd: string | null;
  preferredTimeStart: string | null;
  preferredTimeEnd: string | null;
  durationMinutes: number | null;
};

export type SlotSuggestion = {
  doctorId?: string | null;
  doctorName?: string | null;
  specialty?: string | null;
  facilityId?: string | null;
  facilityName?: string | null;
  date?: string | null;
  startTime?: string | null;
  endTime?: string | null;
  durationMinutes?: number | null;
  reason?: string | null;
};

export type SuggestResponse = {
  suggestions?: SlotSuggestion[];
  alternatives?: SlotSuggestion[];
  message?: string;
};