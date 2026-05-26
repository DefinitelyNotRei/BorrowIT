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

### POST /api/register
Request body:
- `firstName` (string)
- `middleName` (string, optional)
- `lastName` (string)
- `suffix` (string, optional)
- `studentId` (string)
- `phoneNumber` (string)
- `branch` (string)
- `course` (string)
- `yearLevel` (string)
- `block` (string)
- `password` (string)

Response:
- `message` (string)

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

## Password

### POST /api/change-password
Request body:
- `currentPassword` (string)
- `newPassword` (string)

Response:
- `message` (string)

## Notes
- All routes under `/api` that require authentication use session cookies.
- User registration automatically generates `email` as `<studentId>@gordoncollege.edu.ph`.
- Only `USER` and `STUDENT` roles are allowed to login through the user portal.
- Admin-only workflows remain in the existing JavaFX app.
