import SignOutButton from "./Shared/SignOutButton";

export default function AdminHeader() {
  return (
    <header style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 16 }}>
      <div>
        <h1
          style={{
            fontSize: "2.25rem",
            fontWeight: 800,
            color: "#ef4444",
            margin: 0,
          }}
        >
          Admin Portal
        </h1>

        <p style={{ color: "#6b7280", marginTop: "0.5rem", marginBottom: 0 }}>
          Manage doctors, facilities, and rooms
        </p>
      </div>

      <div>
        <SignOutButton />
      </div>
    </header>
  );
}
