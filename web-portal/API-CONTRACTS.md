# BorrowIT User Portal API Contracts

## Authentication

### POST /api/login
Request body:
- `username` (string)
- `password` (string)

Response:
- `id` (number)
- `fullName` (string)
- `username` (string)
- `email` (string)
- `role` (string)

<!-- Registration endpoint removed from the public user API. Account creation is admin-only via the JavaFX admin application. -->

### POST /api/logout
Response:
- `message` (string)

### GET /api/user
Response:
- `user` (object|null)

## Equipment

### GET /api/equipment
Query string:
- `search` (string, optional)

Response:
- `equipment` (array of objects)
  - `equipment_id`
  - `asset_tag`
  - `name`
  - `category`
  - `description`
  - `status`
  - `total_quantity`
  - `available_quantity`

## Reservations

### POST /api/reservations
Request body:
- `equipmentId` (number)
- `quantity` (number)
- `remarks` (string, optional)

Response:
- `message` (string)

### GET /api/reservations/current
Response:
- `reservations` (array of objects)
  - `reservation_id`
  - `equipment_id`
  - `quantity`
  - `status`
  - `request_date`
  - `due_date`
  - `return_date`
  - `name`
  - `asset_tag`

### GET /api/reservations/pending
Response:
- `reservations` (array of objects)
  - `reservation_id`
  - `equipment_id`
  - `quantity`
  - `status`
  - `request_date`
  - `remarks`
  - `name`
  - `asset_tag`

### GET /api/reservations/history
Response:
- `reservations` (array of objects)
  - `reservation_id`
  - `equipment_id`
  - `quantity`
  - `status`
  - `request_date`
  - `due_date`
  - `return_date`
  - `approved_at`
  - `remarks`
  - `name`
  - `asset_tag`

### DELETE /api/reservations/:id
Response:
- `message` (string)

## Admin

### POST /api/admin/users
Request body:
- `firstName` (string)
- `middleName` (string, optional)
- `lastName` (string)
- `suffix` (string, optional)
- `userId` (string)
- `phoneNumber` (string)
- `department` (string)
- `course` (string)
- `yearLevel` (number)
- `block` (string)
- `password` (string)

Response:
- `message` (string)

## Password

### POST /api/change-password
Request body:
- `currentPassword` (string)
- `newPassword` (string)

Response:
- `message` (string)

## Notes
All routes under `/api` that require authentication use session cookies.
Account creation is handled by admin users in the JavaFX admin application or via the admin-only `/api/admin/users` endpoint; there is no public registration endpoint.
Only `USER` and `STUDENT` roles are allowed to login through the user portal.
Admin-only workflows remain in the existing JavaFX app.
