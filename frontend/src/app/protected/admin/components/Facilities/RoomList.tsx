import { Room } from "../../types";
import Badge from "../Shared/Badge";

export default function RoomList({ rooms }: { rooms?: Room[] }) {
  const list = rooms ?? [];
  if (!list.length) return <p>No rooms</p>;

  return (
    <div>
      {list.map((r) => (
        <Badge key={r.id} text={`${r.name} • ${r.specialty}`} />
      ))}
    </div>
  );
}