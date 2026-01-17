export default function AdminHeader() {
  return (
    <div>
      <h1
        style={{
          fontSize: "2.25rem",
          fontWeight: 800,
          color: "#ef4444",
        }}
      >
        Admin Portal
      </h1>

      <p style={{ color: "#6b7280", marginTop: "0.5rem" }}>
        Manage doctors, facilities, and rooms
      </p>
    </div>
  );
}
