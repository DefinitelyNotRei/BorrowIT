# BorrowIT Borrower Portal API Contracts

All endpoints are borrower/student only. Staff and admin workflows remain exclusively in the JavaFX desktop application.

Base path: `/api`

## Authentication

### POST `/login`
Request:
- `username` string, student ID
- `password` string

Response:
- `id`, `fullName`, `username`, `email`, `branch`, `course`, `block`, `yearLevel`, `phoneNumber`, `role`

Notes:
- Only `USER` and `STUDENT` roles can authenticate through the portal.
- `ADMIN` accounts are not selected by the login query.

### POST `/register`
Request:
- `fullName`
- `username`
- `email`
- `phoneNumber`
- `branch`
- `course`
- `block`
- `yearLevel`
- `password`

Response:
- `message`

Notes:
- Student self-registration is disabled for the borrower portal.
- New accounts are created by administrators in the desktop application.

### POST `/verify-email`
Request:
- `token`

Response:
- `message`

### POST `/forgot-password`
Request:
- `email`

Response:
- `message`
- `devResetToken` outside production

### POST `/reset-password`
Request:
- `token`
- `newPassword`

Response:
- `message`

### POST `/logout`
Response:
- `message`

### GET `/user`
Response:
- `user` object or `null`

## Dashboard

### GET `/dashboard`
Response:
- `summary.pending`
- `summary.borrowed`
- `summary.overdue`
- `summary.totalReservations`
- `recent[]`
- `dueSoon[]`

## Profile

### GET `/profile`
Response:
- `user`

### PUT `/profile`
Request:
- `fullName`
- `email`
- `branch`
- `course`
- `block`
- `yearLevel`
- `phoneNumber`

Response:
- `message`
- `user`

### POST `/change-password`
Request:
- `currentPassword`
- `newPassword`

Response:
- `message`

## Equipment

### GET `/equipment/categories`
Response:
- `categories[]`

### GET `/equipment`
Query:
- `search`
- `category`
- `sort`: `name`, `category`, or `available_quantity`
- `page`
- `limit`

Response:
- `equipment[]`
- `pagination`

### GET `/equipment/:id`
Response:
- `equipment`

## Reservations

### POST `/reservations`
Request:
- `equipmentId`
- `quantity`
- `remarks` optional

Response:
- `message`
- `reservationId`
- `referenceNumber`

### GET `/reservations/current`
Response:
- `reservations[]`

### GET `/reservations/pending`
Response:
- `reservations[]`

### GET `/reservations/history`
Response:
- `reservations[]`

### GET `/reservations/:id/receipt`
Response:
- `receipt`

### DELETE `/reservations/:id`
Response:
- `message`

## Notifications And Overdues

### GET `/notifications`
Response:
- `notifications[]`

### GET `/overdues`
Response:
- `overdues[]`

## Security Notes

- Session cookie: `borrowit_sid`, `httpOnly`, `sameSite=lax`, secure in production.
- Password hashing remains PBKDF2-compatible with the JavaFX app. A BCrypt migration should be coordinated across both clients.
- Rate limiting is applied globally under `/api` and more strictly to `/api/login`.
- SQL uses parameterized queries.
