export default function IconButton({
  icon,
  onClick
}: {
  icon: React.ReactNode;
  onClick: () => void;
}) {
  return (
    <button onClick={onClick}
      style={{ background: "transparent", border: "none" }}>
      {icon}
    </button>
  );
}
