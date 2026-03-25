# wecureit

Wecureit provides a backend analytics service and a Next.js frontend focused on referral workflows between healthcare providers. It includes:

- A Spring Boot (Maven) backend (`backend/`) with REST APIs, Neo4j analytics integration and DB seeding scripts.
- A Next.js frontend (`frontend/`) for UI and admin flows.
- Playwright end-to-end tests in `e2e/` and test scenarios / seed scripts in `backend/scripts/`.

This README explains what the project does and how to run it locally.

**Project Structure**

- `backend/` — Spring Boot application (JDK 21, Maven). See [backend/README.md](backend/README.md) for backend-specific details and Neo4j seeding instructions.
- `frontend/` — Next.js app. See [frontend/README.md](frontend/README.md) for frontend-specific details.

## Quick start (development)

Prerequisites

- Java 21 (JDK 21) for the backend
- Maven (or use the Maven wrapper in `backend/` if present)
- Node.js (v18+ recommended) and an npm/yarn/pnpm client for the frontend and e2e tests
- PostgreSQL database for the backend (or configure `DATABASE_URL`)
- Neo4j for analytics features (optional for basic UI)

1) Configure secrets and credentials

- Edit `backend/src/main/resources/application.properties` and set your PostgreSQL and Neo4j credentials (or set env vars). Do NOT commit secrets 
- The frontend uses Firebase configuration in `frontend/src/lib/firebase.ts`. Replace the `apiKey` and other values with your project values or use environment-driven configuration.

2) Run the backend

```bash
cd backend
# either with your system mvn
mvn spring-boot:run
# or (if provided) with the wrapper
./mvnw spring-boot:run
```

The backend runs by default on `http://localhost:8080`.

3) Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:3000` to view the app.

