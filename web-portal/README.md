# BorrowIT Borrower Web Portal

Responsive borrower/student portal for equipment browsing, reservation requests, borrowed item tracking, reservation history, profile management, notifications, registration, and password recovery.

Staff and admin workflows are not implemented in this portal. They remain in the standalone JavaFX desktop application.

## Run

```bash
npm install
npm start
```

Open `http://localhost:3000`.

## Environment

Copy `.env.example` to `.env` and set:

- `SESSION_SECRET`
- `DB_HOST`
- `DB_PORT`
- `DB_USER`
- `DB_PASSWORD`
- `DB_NAME`
- `CORS_ORIGIN` only when serving the frontend from another origin
- `NODE_ENV=production` for secure cookies and hidden development tokens

## Database

For a fresh database, run:

```sql
SOURCE database/borrowit_schema.sql;
SOURCE database/borrowit_seed.sql;
SOURCE database/borrowit_sample_data.sql;
```

For an existing database, run:

```sql
SOURCE database/borrowit_modernization_migration.sql;
```

## Current Implementation

- Express REST API
- MySQL shared with the JavaFX desktop application
- Session-cookie authentication for borrower portal
- PBKDF2 password hashing compatible with the JavaFX app
- Rate limiting, Helmet CSP, input sanitization, parameterized SQL
- Mobile-first academic portal UI with sidebar/topbar layout

## Target Modernization Path

The long-term target in `MODERNIZATION_BLUEPRINT.md` is a React/Vite/Tailwind frontend plus a Spring Boot API. This Express portal is the working borrower-only implementation that preserves the current JavaFX compatibility while the target stack is migrated.
