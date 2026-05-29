# BorrowIT Modernization Blueprint

Updated: May 29, 2026

## 1. Full System Architecture

BorrowIT is a hybrid system with a hard boundary between staff operations and borrower self-service.

```text
[ Borrower Web Portal ]
        |
        | HTTPS + REST API
        v
[ Borrower API Layer ]
        |
        | JDBC/MySQL driver
        v
[ Shared MySQL Database ]
        ^
        | JDBC
        |
[ JavaFX Admin Desktop Application ]
```

Rules:
- The borrower web portal contains no admin pages and no admin routes.
- Admin/staff features remain in JavaFX.
- JavaFX remains operational if the web portal is offline.
- The database is the shared integration contract.

Current implementation:
- JavaFX admin app in `src/main/java/com/borrowit`.
- Borrower portal/API in `web-portal`.
- Shared schema in `database`.

Target implementation:
- `BorrowIT-Web/frontend`: React, Vite, Tailwind CSS, Axios, React Router.
- `BorrowIT-Web/backend`: Spring Boot REST API with controller/service/repository layers.
- `BorrowIT-Admin`: JavaFX desktop application, JDBC or secured internal API.

## 2. Database ERD Improvements

Core entities:
- `users`: students, staff/admin account identity, account status, verification and reset fields.
- `admins`: staff/admin profile linked to `users`.
- `equipment`: inventory, category, quantity, image/location metadata.
- `reservations`: request lifecycle, reference number, approval/return/due timestamps.
- `overdues`: durable overdue incidents and penalty windows.
- `notifications`: borrower notification center.
- `activity_logs`: audit trail for sensitive actions.
- `reservation_status_history`: status timeline for approvals, declines, returns, and cancellations.

Relationships:
- `admins.user_id -> users.user_id`
- `reservations.user_id -> users.user_id`
- `reservations.equipment_id -> equipment.equipment_id`
- `reservations.processed_by_admin_id -> admins.admin_id`
- `overdues.reservation_id -> reservations.reservation_id`
- `notifications.user_id -> users.user_id`
- `reservation_status_history.reservation_id -> reservations.reservation_id`

## 3. Backend API Architecture

Recommended Spring Boot package layout:

```text
backend/
  controllers/
    AuthenticationController
    UserController
    EquipmentController
    ReservationController
    NotificationController
  services/
    AuthenticationService
    UserService
    EquipmentService
    ReservationService
    NotificationService
    AuditLogService
  repositories/
    UserRepository
    EquipmentRepository
    ReservationRepository
    NotificationRepository
  models/
  dto/
  security/
  config/
  utilities/
```

Controller rules:
- Thin controllers.
- DTO validation at the boundary.
- Business rules in services.
- Repository layer owns database access.
- Standard error responses across all endpoints.

## 4. Frontend Architecture

Recommended React structure:

```text
frontend/src/
  pages/
    Dashboard
    Equipment
    EquipmentDetails
    PendingRequests
    CurrentBorrowings
    History
    Account
    Login
    Register
    ForgotPassword
    ResetPassword
  components/
    AppShell
    Sidebar
    Topbar
    StatusBadge
    EquipmentCard
    ReservationRow
    EmptyState
    LoadingState
  layouts/
  services/
    apiClient
    authService
    reservationService
    equipmentService
  hooks/
  context/
  assets/
  styles/
```

Current portal implementation mirrors that structure with static pages plus `web-portal/public/js/app.js`.

## 5. UI/UX Implementation Plan

Layout:
- Mobile-first portal shell.
- Sidebar navigation on desktop.
- Topbar for page title, account, and notifications.
- Dashboard cards for pending, borrowed, overdue, and total reservations.
- Dense list rows for academic/student workflows.

Design principles:
- Academic blue primary system using `#0F3D91`, `#1E56C5`, `#FFC107`, `#F5F7FA`, `#1F2937`.
- 8px card radius.
- Clear loading, empty, success, and error states.
- Keyboard-visible focus rings.
- Responsive grids for mobile, tablet, and desktop.

## 6. Security Implementation Plan

Implemented now:
- Borrower-only route isolation.
- Admin accounts excluded from web login queries.
- Session cookie with `httpOnly`, `sameSite=lax`, secure in production.
- Rate limiting on API and stricter login limiting.
- Helmet CSP.
- Input sanitization and parameterized SQL.
- PBKDF2 password hashing compatible with the JavaFX app.

Target production upgrades:
- JWT access token plus refresh token rotation, or server-side sessions backed by Redis.
- BCrypt or Argon2 migration coordinated across JavaFX and web.
- CSRF tokens for cookie-authenticated state-changing requests.
- SMTP-backed verification and reset delivery.
- Audit logging for login, reservation, profile, and password actions.
- Role guards enforced centrally.
- Secrets only through environment variables.

## 7. Folder Structures

Recommended target:

```text
BorrowIT-Web/
  frontend/
    src/pages/
    src/components/
    src/layouts/
    src/services/
    src/hooks/
    src/context/
    src/assets/
    src/styles/
  backend/
    controllers/
    services/
    repositories/
    models/
    dto/
    security/
    config/
    utilities/

BorrowIT-Admin/
  dashboard/
  reservations/
  equipment/
  users/
  reports/
  database/
  utilities/
```

Current repository mapping:
- Root Maven project is the JavaFX admin application.
- `web-portal` is the borrower web portal/API.
- `database` stores schema, seed data, indexes, and modernization migration.

## 8. API Endpoint Documentation

Borrower endpoints are documented in `web-portal/API-CONTRACTS.md`.

Groups:
- Authentication: login, register, verify email, forgot/reset password, logout.
- Dashboard: summary, recent activity, due soon.
- Equipment: list, categories, details.
- Reservations: create, pending, current, history, cancel, receipt.
- Profile: read/update profile, change password.
- Notifications: borrower notification feed.
- Overdues: borrower overdue records.

No admin endpoint belongs in the borrower API.

## 9. Database Schema Improvements

Implemented in `database/borrowit_schema.sql` and `database/borrowit_modernization_migration.sql`:
- `users.account_status`
- email verification token/hash fields
- password reset token/hash fields
- profile image field
- equipment image/location fields
- reservation reference numbers
- reservation expiration timestamps
- notification table
- activity log table
- reservation status history table
- supporting indexes

## 10. Responsive Design Strategy

Breakpoints:
- Mobile: single-column shell, horizontal nav, stacked cards.
- Tablet: two-column cards where useful.
- Desktop: fixed sidebar, sticky topbar, multi-column dashboard/equipment grid.

Rules:
- No text overlap.
- Stable grid/card dimensions.
- Buttons and form fields remain tappable.
- Empty/loading states keep layout stable while data loads.

## 11. Authentication Flow

Student login:
1. Student submits ID and password.
2. API checks only `USER` or `STUDENT` roles.
3. Password hash is verified.
4. Borrower session is created.
5. Portal redirects to dashboard.

Registration:
1. Student submits registration form.
2. API creates `STUDENT` account.
3. Modern schema stores a verification token hash and marks account pending.
4. Verification endpoint activates account.
5. Production SMTP should deliver the verification link.

Password reset:
1. Student submits email.
2. API stores reset token hash and expiration.
3. Production SMTP sends reset link.
4. Student submits token and new password.

## 12. Reservation Workflow

1. Student browses available equipment.
2. Student submits reservation quantity.
3. API validates availability and active penalties.
4. Reservation is created as `PENDING`.
5. JavaFX staff app approves or declines.
6. Approval decreases available equipment quantity.
7. Student sees status update in dashboard/history/notifications.
8. Staff processes return in JavaFX.
9. Late returns create overdue records and penalties.

## 13. Deployment Strategy

Development:
- MySQL locally.
- JavaFX via Maven.
- Web portal via `npm start`.

Production target:
- MySQL with least-privilege database users.
- Web API behind HTTPS reverse proxy.
- Environment variables for secrets.
- JavaFX deployed internally to staff machines.
- Database backups scheduled daily.
- Static frontend deployed behind CDN or school server.

## 14. Testing Strategy

Backend:
- Unit tests for services.
- Integration tests for API endpoints.
- Authentication/authorization tests.
- SQL migration tests.

Frontend:
- Component tests for portal controls.
- E2E tests for login, search, request, cancel, profile update, password reset.
- Responsive screenshots for mobile/tablet/desktop.

Security:
- Rate-limit test.
- SQL injection test payloads.
- XSS payload tests.
- Session expiration checks.
- Role isolation test proving admin users cannot enter portal.

## 15. Scalability Recommendations

- Add Redis for session storage and rate-limit persistence.
- Add pagination to all list endpoints.
- Add full-text search for equipment.
- Add async notification/email queue.
- Store uploaded profile/equipment images in object storage.
- Add API versioning under `/api/v1`.
- Add OpenAPI/Swagger documentation.

## 16. Feature Enhancement Recommendations

- QR code per reservation receipt.
- Email notifications for approval, decline, due soon, overdue.
- WebSocket or server-sent events for live reservation updates.
- Reservation timeline page.
- Calendar view for borrowed/due items.
- Equipment image gallery.
- Dark mode after the core UI is stable.
- CSV/PDF export from JavaFX reports.

## 17. Production-Ready Coding Standards

- Keep admin and borrower code physically and logically separated.
- Validate every request server-side.
- Never concatenate user input into SQL.
- Use DTOs for API input/output.
- Use service transactions for reservation approval/return operations.
- Standardize API errors.
- Centralize security rules.
- Keep secrets out of source control.
- Maintain migration scripts for every schema change.
- Add tests for role isolation and reservation state transitions.
