# BorrowIT User Portal

A lightweight web portal for BorrowIT user workflows that shares the same MySQL schema as the existing JavaFX admin app.

## Features
- User registration and login
- Browse available equipment
- Search equipment by name / asset tag / description
- Submit reservation requests
- View current approved loans
- View reservation history
- Cancel pending reservation requests
- Change password

## Installation
1. Copy `.env.example` to `.env`
2. Install dependencies
   ```bash
   npm install
   ```
3. Run the portal
   ```bash
   npm start
   ```
4. Open `http://localhost:3000`

## API Endpoints
- `POST /api/login`
- `POST /api/register`
- `POST /api/logout`
- `GET /api/user`
- `GET /api/equipment`
- `POST /api/reservations`
- `GET /api/reservations/current`
- `GET /api/reservations/history`
- `DELETE /api/reservations/:id`
- `POST /api/change-password`

## Notes
- This portal uses the same `users`, `equipment`, and `reservations` tables as the existing JavaFX admin app.
- Admin-only actions remain in the desktop JavaFX app.
