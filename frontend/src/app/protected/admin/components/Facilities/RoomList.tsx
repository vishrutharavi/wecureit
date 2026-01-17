import { Room } from "../../types";
import Badge from "../Shared/Badge";

export default function RoomList({ rooms }: { rooms: Room[] }) {
  if (!rooms.length) return <p>No rooms</p>;

  return (
    <div>
      {rooms.map(r => (
        <Badge key={r.id} text={`${r.name} • ${r.specialty}`} />
      ))}
    </div>
  );
}