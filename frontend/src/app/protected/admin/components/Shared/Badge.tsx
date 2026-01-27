export default function Badge({ text }: { text: string }) {
  return (
    <span style={{
      background: "#fee2e2",
      color: "#991b1b",
      padding: "4px 10px",
      borderRadius: "999px",
      marginRight: "6px",
      fontSize: "0.75rem"
    }}>
      {text}
    </span>
  );
}
