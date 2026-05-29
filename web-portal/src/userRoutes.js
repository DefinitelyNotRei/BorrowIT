const crypto = require('crypto');
const express = require('express');
const db = require('./db');
const { hashPassword, verifyPassword, validatePasswordStrength } = require('./passwordHasher');
const { recordFailedAttempt, isLockedOut, clearFailedAttempts, getRemainingAttempts } = require('./authLimiter');

const router = express.Router();
const legacyResetTokens = new Map();

let userModernColumns = null;
let reservationModernColumns = null;

function asyncRoute(handler) {
  return (req, res, next) => Promise.resolve(handler(req, res, next)).catch(next);
}

function clean(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function cleanUpper(value) {
  return clean(value).toUpperCase();
}

function hashOpaqueToken(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

function generateToken(bytes = 32) {
  return crypto.randomBytes(bytes).toString('hex');
}

function generateReferenceNumber() {
  const date = new Date();
  const ymd = [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, '0'),
    String(date.getDate()).padStart(2, '0')
  ].join('');
  return `BRW-${ymd}-${crypto.randomBytes(3).toString('hex').toUpperCase()}`;
}

async function hasColumn(table, column) {
  const [rows] = await db.query(`SHOW COLUMNS FROM ${table} LIKE ?`, [column]);
  return rows.length > 0;
}

async function hasModernUserColumns() {
  if (userModernColumns === null) {
    userModernColumns = await hasColumn('users', 'account_status');
  }
  return userModernColumns;
}

async function hasModernReservationColumns() {
  if (reservationModernColumns === null) {
    reservationModernColumns = await hasColumn('reservations', 'reference_number');
  }
  return reservationModernColumns;
}

function requireAuth(req, res, next) {
  if (req.session?.user?.id) {
    return next();
  }
  return res.status(401).json({ message: 'Authentication required.' });
}

function requireStudentSession(req, res, next) {
  if (!req.session?.user?.id) {
    return res.status(401).json({ message: 'Authentication required.' });
  }
  if (!['USER', 'STUDENT'].includes(req.session.user.role)) {
    req.session.destroy(() => {});
    return res.status(403).json({ message: 'Borrower portal access only.' });
  }
  return next();
}

function validateEmail(email) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}

function validateRegistrationPayload(payload) {
  const fullName = clean(payload.fullName);
  const username = clean(payload.username);
  const email = clean(payload.email).toLowerCase();
  const branch = clean(payload.branch) || 'General';
  const course = clean(payload.course) || 'General';
  const block = cleanUpper(payload.block) || 'A';
  const yearLevel = Number(payload.yearLevel || 1);
  const phoneNumber = clean(payload.phoneNumber);
  const password = payload.password;

  if (!/^[A-Za-z .'-]{3,120}$/.test(fullName)) {
    return { error: 'Full name must be 3-120 characters and contain valid name characters.' };
  }
  if (!/^\d{3,50}$/.test(username)) {
    return { error: 'Student ID must contain digits and be at least 3 characters.' };
  }
  if (!validateEmail(email)) {
    return { error: 'Enter a valid email address.' };
  }
  if (!/^[A-Z]$/.test(block)) {
    return { error: 'Block must be a single letter.' };
  }
  if (!Number.isInteger(yearLevel) || yearLevel < 1 || yearLevel > 4) {
    return { error: 'Year level must be between 1 and 4.' };
  }
  if (!/^\d{11}$/.test(phoneNumber)) {
    return { error: 'Phone number must be 11 digits.' };
  }

  const passwordValidation = validatePasswordStrength(password);
  if (!passwordValidation.valid) {
    return { error: passwordValidation.message };
  }

  return {
    data: {
      fullName,
      username,
      email,
      branch,
      course,
      block,
      yearLevel,
      phoneNumber,
      password
    }
  };
}

function toPublicUser(user) {
  return {
    id: user.user_id,
    fullName: user.full_name,
    username: user.username,
    email: user.email,
    branch: user.branch,
    course: user.course,
    block: user.block,
    yearLevel: user.year_level,
    phoneNumber: user.phone_number,
    role: user.role === 'STUDENT' ? 'USER' : user.role
  };
}

router.post('/login', asyncRoute(async (req, res) => {
  const username = clean(req.body.username);
  const { password } = req.body;
  const ip = req.ip || req.connection.remoteAddress;

  if (!username || !password) {
    return res.status(400).json({ message: 'Username and password are required.' });
  }

  const lockoutStatus = isLockedOut(ip, username);
  if (lockoutStatus) {
    return res.status(429).json({
      message: `Account locked due to too many failed attempts. Please try again in ${lockoutStatus.remainingTime} minutes.`,
      locked: true,
      remainingTime: lockoutStatus.remainingTime
    });
  }

  const modernUsers = await hasModernUserColumns();
  const optionalColumns = modernUsers ? ', account_status, email_verified_at' : '';
  const [rows] = await db.query(
    `SELECT user_id, full_name, username, email, branch, course, block, year_level, phone_number, role, is_active, password_hash${optionalColumns}
     FROM users
     WHERE username = ? AND role IN ('USER', 'STUDENT')`,
    [username]
  );

  const user = rows[0];
  if (!user || !user.is_active || (modernUsers && user.account_status !== 'ACTIVE')) {
    recordFailedAttempt(ip, username);
    return res.status(401).json({
      message: 'Invalid credentials or account is not active.',
      remainingAttempts: getRemainingAttempts(ip, username)
    });
  }

  if (!verifyPassword(password, user.password_hash)) {
    recordFailedAttempt(ip, username);
    return res.status(401).json({
      message: 'Invalid credentials.',
      remainingAttempts: getRemainingAttempts(ip, username)
    });
  }

  clearFailedAttempts(ip, username);

  req.session.user = toPublicUser(user);
  return res.json(req.session.user);
}));

router.post('/register', asyncRoute(async (req, res) => {
  return res.status(403).json({
    message: 'Student self-registration is disabled. Contact your administrator to request an account.'
  });
}));

router.post('/verify-email', asyncRoute(async (req, res) => {
  const token = clean(req.body.token || req.query.token);
  if (!token) {
    return res.status(400).json({ message: 'Verification token is required.' });
  }

  const modernUsers = await hasModernUserColumns();
  if (!modernUsers) {
    return res.json({ message: 'Email verification is not required on the current schema.' });
  }

  const [result] = await db.query(
    `UPDATE users
     SET account_status = 'ACTIVE',
         is_active = 1,
         email_verified_at = NOW(),
         verification_token_hash = NULL,
         verification_sent_at = NULL,
         updated_at = CURRENT_TIMESTAMP
     WHERE verification_token_hash = ? AND account_status = 'PENDING_VERIFICATION'`,
    [hashOpaqueToken(token)]
  );

  if (result.affectedRows === 0) {
    return res.status(400).json({ message: 'Verification link is invalid or already used.' });
  }

  return res.json({ message: 'Email verified. You can now sign in.' });
}));

router.post('/forgot-password', asyncRoute(async (req, res) => {
  const email = clean(req.body.email).toLowerCase();
  if (!validateEmail(email)) {
    return res.status(400).json({ message: 'Enter a valid email address.' });
  }

  const resetToken = generateToken();
  const modernUsers = await hasModernUserColumns();

  if (modernUsers) {
    await db.query(
      `UPDATE users
       SET password_reset_token_hash = ?,
           password_reset_expires_at = DATE_ADD(NOW(), INTERVAL 30 MINUTE),
           updated_at = CURRENT_TIMESTAMP
       WHERE email = ? AND role IN ('USER', 'STUDENT')`,
      [hashOpaqueToken(resetToken), email]
    );
  } else {
    legacyResetTokens.set(resetToken, { email, expiresAt: Date.now() + 30 * 60 * 1000 });
  }

  return res.json({
    message: 'If the email belongs to a borrower account, reset instructions will be sent.',
    devResetToken: process.env.NODE_ENV === 'production' ? undefined : resetToken
  });
}));

router.post('/reset-password', asyncRoute(async (req, res) => {
  const token = clean(req.body.token);
  const { newPassword } = req.body;

  if (!token || !newPassword) {
    return res.status(400).json({ message: 'Reset token and new password are required.' });
  }

  const passwordValidation = validatePasswordStrength(newPassword);
  if (!passwordValidation.valid) {
    return res.status(400).json({ message: passwordValidation.message });
  }

  const passwordHash = hashPassword(newPassword);
  const modernUsers = await hasModernUserColumns();

  if (modernUsers) {
    const [result] = await db.query(
      `UPDATE users
       SET password_hash = ?,
           password_reset_token_hash = NULL,
           password_reset_expires_at = NULL,
           account_status = 'ACTIVE',
           is_active = 1,
           updated_at = CURRENT_TIMESTAMP
       WHERE password_reset_token_hash = ?
         AND password_reset_expires_at > NOW()
         AND role IN ('USER', 'STUDENT')`,
      [passwordHash, hashOpaqueToken(token)]
    );

    if (result.affectedRows === 0) {
      return res.status(400).json({ message: 'Reset token is invalid or expired.' });
    }

    return res.json({ message: 'Password reset successfully.' });
  }

  const tokenRecord = legacyResetTokens.get(token);
  if (!tokenRecord || tokenRecord.expiresAt < Date.now()) {
    legacyResetTokens.delete(token);
    return res.status(400).json({ message: 'Reset token is invalid or expired.' });
  }

  const [result] = await db.query(
    `UPDATE users
     SET password_hash = ?, updated_at = CURRENT_TIMESTAMP
     WHERE email = ? AND role IN ('USER', 'STUDENT')`,
    [passwordHash, tokenRecord.email]
  );
  legacyResetTokens.delete(token);

  if (result.affectedRows === 0) {
    return res.status(400).json({ message: 'Reset token is invalid or expired.' });
  }

  return res.json({ message: 'Password reset successfully.' });
}));

router.post('/logout', (req, res) => {
  req.session.destroy(err => {
    if (err) {
      return res.status(500).json({ message: 'Unable to logout.' });
    }
    res.clearCookie('borrowit_sid');
    return res.json({ message: 'Logged out.' });
  });
});

router.get('/user', (req, res) => {
  if (!req.session?.user) {
    return res.status(200).json({ user: null });
  }
  return res.json({ user: req.session.user });
});

router.get('/dashboard', requireStudentSession, asyncRoute(async (req, res) => {
  const userId = req.session.user.id;
  const [[summary]] = await db.query(
    `SELECT
       SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) AS pending_count,
       SUM(CASE WHEN status = 'APPROVED' THEN 1 ELSE 0 END) AS borrowed_count,
       SUM(CASE WHEN status = 'APPROVED' AND due_date IS NOT NULL AND due_date < NOW() THEN 1 ELSE 0 END) AS overdue_count,
       COUNT(*) AS total_count
     FROM reservations
     WHERE user_id = ?`,
    [userId]
  );

  const [recent] = await db.query(
    `SELECT r.reservation_id, r.status, r.request_date, r.due_date, r.quantity,
            e.name, e.asset_tag
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ?
     ORDER BY r.request_date DESC
     LIMIT 5`,
    [userId]
  );

  const [dueSoon] = await db.query(
    `SELECT r.reservation_id, r.due_date, e.name, e.asset_tag
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ?
       AND r.status = 'APPROVED'
       AND r.due_date IS NOT NULL
       AND r.due_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 3 DAY)
     ORDER BY r.due_date ASC`,
    [userId]
  );

  return res.json({
    summary: {
      pending: Number(summary.pending_count || 0),
      borrowed: Number(summary.borrowed_count || 0),
      overdue: Number(summary.overdue_count || 0),
      totalReservations: Number(summary.total_count || 0)
    },
    recent,
    dueSoon
  });
}));

router.get('/profile', requireStudentSession, asyncRoute(async (req, res) => {
  const [rows] = await db.query(
    `SELECT user_id, full_name, username, email, branch, course, block, year_level, phone_number, role
     FROM users
     WHERE user_id = ? AND role IN ('USER', 'STUDENT')`,
    [req.session.user.id]
  );

  const user = rows[0];
  if (!user) {
    return res.status(404).json({ message: 'Profile not found.' });
  }

  return res.json({ user: toPublicUser(user) });
}));

router.put('/profile', requireStudentSession, asyncRoute(async (req, res) => {
  const phoneNumber = clean(req.body.phoneNumber);

  if (!/^\d{11}$/.test(phoneNumber)) {
    return res.status(400).json({ message: 'Phone number must be 11 digits.' });
  }

  await db.query(
    `UPDATE users
     SET phone_number = ?, updated_at = CURRENT_TIMESTAMP
     WHERE user_id = ? AND role IN ('USER', 'STUDENT')`,
    [phoneNumber, req.session.user.id]
  );

  req.session.user = {
    ...req.session.user,
    phoneNumber
  };

  return res.json({ message: 'Profile updated.', user: req.session.user });
}));

router.get('/equipment/categories', requireStudentSession, asyncRoute(async (req, res) => {
  const [rows] = await db.query(
    `SELECT DISTINCT category
     FROM equipment
     WHERE category IS NOT NULL AND category <> ''
     ORDER BY category ASC`
  );
  return res.json({ categories: rows.map(row => row.category) });
}));

router.get('/equipment/:id', requireStudentSession, asyncRoute(async (req, res) => {
  const equipmentId = Number(req.params.id);
  if (!Number.isInteger(equipmentId) || equipmentId <= 0) {
    return res.status(400).json({ message: 'Invalid equipment id.' });
  }

  const [rows] = await db.query(
    `SELECT equipment_id, asset_tag, name, category, description, status, total_quantity, available_quantity, created_at, updated_at
     FROM equipment
     WHERE equipment_id = ?`,
    [equipmentId]
  );

  if (!rows[0]) {
    return res.status(404).json({ message: 'Equipment not found.' });
  }

  return res.json({ equipment: rows[0] });
}));

router.get('/equipment', requireStudentSession, asyncRoute(async (req, res) => {
  const search = clean(req.query.search);
  const category = clean(req.query.category);
  const sort = ['name', 'category', 'available_quantity'].includes(req.query.sort) ? req.query.sort : 'name';
  const page = Math.max(parseInt(req.query.page, 10) || 1, 1);
  const limit = Math.min(Math.max(parseInt(req.query.limit, 10) || 12, 1), 50);
  const offset = (page - 1) * limit;

  const where = ['status = ?', 'available_quantity > 0'];
  const params = ['AVAILABLE'];

  if (search) {
    where.push('(name LIKE ? OR asset_tag LIKE ? OR description LIKE ? OR category LIKE ?)');
    params.push(`%${search}%`, `%${search}%`, `%${search}%`, `%${search}%`);
  }

  if (category) {
    where.push('category = ?');
    params.push(category);
  }

  const whereSql = where.join(' AND ');
  const [[countResult]] = await db.query(
    `SELECT COUNT(*) AS total FROM equipment WHERE ${whereSql}`,
    params
  );
  const total = Number(countResult.total || 0);

  const [rows] = await db.query(
    `SELECT equipment_id, asset_tag, name, category, description, status, total_quantity, available_quantity
     FROM equipment
     WHERE ${whereSql}
     ORDER BY ${sort} ASC, name ASC
     LIMIT ? OFFSET ?`,
    [...params, limit, offset]
  );

  return res.json({
    equipment: rows,
    pagination: {
      page,
      limit,
      total,
      totalPages: Math.ceil(total / limit),
      hasNext: page < Math.ceil(total / limit),
      hasPrev: page > 1
    }
  });
}));

router.post('/reservations', requireStudentSession, asyncRoute(async (req, res) => {
  const equipmentId = Number(req.body.equipmentId);
  const quantity = Number(req.body.quantity || 1);
  const remarks = clean(req.body.remarks) || null;
  const userId = req.session.user.id;

  if (!Number.isInteger(equipmentId) || equipmentId <= 0) {
    return res.status(400).json({ message: 'Equipment id is required.' });
  }
  if (!Number.isInteger(quantity) || quantity <= 0) {
    return res.status(400).json({ message: 'Quantity must be greater than zero.' });
  }

  const [blocking] = await db.query(
    `SELECT overdue_id, penalty_end_date
     FROM overdues
     WHERE user_id = ? AND settled = 0 AND penalty_end_date IS NOT NULL AND penalty_end_date > NOW()
     LIMIT 1`,
    [userId]
  );
  if (blocking.length > 0) {
    const block = blocking[0];
    return res.status(403).json({
      message: `You have an active penalty. You cannot request new reservations until ${new Date(block.penalty_end_date).toLocaleString()}.`,
      blockedUntil: block.penalty_end_date
    });
  }

  const [equipmentRows] = await db.query(
    'SELECT equipment_id, available_quantity, status FROM equipment WHERE equipment_id = ?',
    [equipmentId]
  );
  const equipment = equipmentRows[0];

  if (!equipment || equipment.status !== 'AVAILABLE' || equipment.available_quantity < quantity) {
    return res.status(400).json({ message: 'Selected equipment is not available.' });
  }

  const modernReservations = await hasModernReservationColumns();
  const referenceNumber = generateReferenceNumber();
  let result;

  if (modernReservations) {
    [result] = await db.query(
      `INSERT INTO reservations (user_id, equipment_id, quantity, status, remarks, reference_number, expires_at)
       VALUES (?, ?, ?, 'PENDING', ?, ?, DATE_ADD(NOW(), INTERVAL 24 HOUR))`,
      [userId, equipmentId, quantity, remarks, referenceNumber]
    );
  } else {
    [result] = await db.query(
      `INSERT INTO reservations (user_id, equipment_id, quantity, status, remarks)
       VALUES (?, ?, ?, 'PENDING', ?)`,
      [userId, equipmentId, quantity, remarks]
    );
  }

  return res.status(201).json({
    message: 'Reservation request submitted.',
    reservationId: result.insertId,
    referenceNumber: modernReservations ? referenceNumber : `BRW-${String(result.insertId).padStart(6, '0')}`
  });
}));

router.get('/reservations/current', requireStudentSession, asyncRoute(async (req, res) => {
  const userId = req.session.user.id;
  const modernReservations = await hasModernReservationColumns();
  const optionalColumns = modernReservations ? ', r.reference_number, r.expires_at' : '';
  const [rows] = await db.query(
    `SELECT r.reservation_id, r.equipment_id, r.quantity, r.status, r.request_date, r.due_date, r.return_date${optionalColumns},
            e.name, e.asset_tag
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ? AND r.status = 'APPROVED'
     ORDER BY r.due_date ASC, r.request_date DESC`,
    [userId]
  );

  return res.json({ reservations: rows });
}));

router.get('/reservations/history', requireStudentSession, asyncRoute(async (req, res) => {
  const userId = req.session.user.id;
  const modernReservations = await hasModernReservationColumns();
  const optionalColumns = modernReservations ? ', r.reference_number, r.expires_at' : '';
  const [rows] = await db.query(
    `SELECT r.reservation_id, r.equipment_id, r.quantity, r.status, r.request_date, r.due_date, r.return_date,
            r.approved_at, r.remarks${optionalColumns}, e.name, e.asset_tag
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ?
     ORDER BY r.request_date DESC`,
    [userId]
  );

  return res.json({ reservations: rows });
}));

router.get('/reservations/pending', requireStudentSession, asyncRoute(async (req, res) => {
  const userId = req.session.user.id;
  const modernReservations = await hasModernReservationColumns();
  const optionalColumns = modernReservations ? ', r.reference_number, r.expires_at' : '';
  const [rows] = await db.query(
    `SELECT r.reservation_id, r.equipment_id, r.quantity, r.status, r.request_date, r.remarks${optionalColumns},
            e.name, e.asset_tag
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ? AND r.status = 'PENDING'
     ORDER BY r.request_date DESC`,
    [userId]
  );

  return res.json({ reservations: rows });
}));

router.get('/reservations/:id/receipt', requireStudentSession, asyncRoute(async (req, res) => {
  const reservationId = Number(req.params.id);
  if (!Number.isInteger(reservationId) || reservationId <= 0) {
    return res.status(400).json({ message: 'Invalid reservation id.' });
  }

  const modernReservations = await hasModernReservationColumns();
  const optionalColumns = modernReservations ? ', r.reference_number, r.expires_at' : '';
  const [rows] = await db.query(
    `SELECT r.reservation_id, r.quantity, r.status, r.request_date, r.approved_at, r.due_date, r.return_date,
            r.remarks${optionalColumns}, e.name, e.asset_tag, e.category, u.full_name, u.username
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     JOIN users u ON r.user_id = u.user_id
     WHERE r.reservation_id = ? AND r.user_id = ?`,
    [reservationId, req.session.user.id]
  );

  const receipt = rows[0];
  if (!receipt) {
    return res.status(404).json({ message: 'Reservation not found.' });
  }

  return res.json({
    receipt: {
      ...receipt,
      reference_number: receipt.reference_number || `BRW-${String(receipt.reservation_id).padStart(6, '0')}`
    }
  });
}));

router.delete('/reservations/:id', requireStudentSession, asyncRoute(async (req, res) => {
  const userId = req.session.user.id;
  const reservationId = Number(req.params.id);

  if (!Number.isInteger(reservationId) || reservationId <= 0) {
    return res.status(400).json({ message: 'Invalid reservation id.' });
  }

  const [rows] = await db.query(
    'SELECT reservation_id, status FROM reservations WHERE reservation_id = ? AND user_id = ?',
    [reservationId, userId]
  );
  const reservation = rows[0];

  if (!reservation) {
    return res.status(404).json({ message: 'Reservation not found.' });
  }

  if (reservation.status !== 'PENDING') {
    return res.status(400).json({ message: 'Only pending reservations may be canceled.' });
  }

  await db.query(
    `UPDATE reservations
     SET status = 'CANCELLED', remarks = 'Cancelled by borrower.', updated_at = CURRENT_TIMESTAMP
     WHERE reservation_id = ?`,
    [reservationId]
  );
  return res.json({ message: 'Reservation canceled.' });
}));

router.get('/overdues', requireStudentSession, asyncRoute(async (req, res) => {
  const userId = req.session.user.id;
  const [rows] = await db.query(
    `SELECT overdue_id, reservation_id, equipment_id, days_late, penalty_days, penalty_end_date, settled, settled_at, created_at
     FROM overdues
     WHERE user_id = ?
     ORDER BY created_at DESC`,
    [userId]
  );
  return res.json({ overdues: rows });
}));

router.get('/notifications', requireStudentSession, asyncRoute(async (req, res) => {
  const userId = req.session.user.id;
  const [reservationEvents] = await db.query(
    `SELECT r.reservation_id, r.status, r.request_date, r.updated_at, r.due_date, e.name
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ?
     ORDER BY COALESCE(r.updated_at, r.request_date) DESC
     LIMIT 10`,
    [userId]
  );

  const [overdueEvents] = await db.query(
    `SELECT o.overdue_id, o.reservation_id, o.days_late, o.penalty_end_date, o.settled, o.created_at
     FROM overdues o
     WHERE o.user_id = ?
     ORDER BY o.created_at DESC
     LIMIT 5`,
    [userId]
  );

  const reservationNotifications = reservationEvents.map(item => ({
    id: `reservation-${item.reservation_id}-${item.status}`,
    type: 'reservation',
    title: `${item.name} request ${String(item.status).toLowerCase()}`,
    message: item.status === 'APPROVED'
      ? `Your reservation was approved. Due date: ${item.due_date || 'to be assigned'}.`
      : `Reservation status is now ${item.status}.`,
    createdAt: item.updated_at || item.request_date,
    read: false
  }));

  const overdueNotifications = overdueEvents.map(item => ({
    id: `overdue-${item.overdue_id}`,
    type: 'overdue',
    title: item.settled ? 'Overdue record settled' : 'Overdue warning',
    message: item.settled
      ? `Reservation ${item.reservation_id} overdue record is settled.`
      : `Reservation ${item.reservation_id} is ${item.days_late} day(s) late. Penalty ends ${item.penalty_end_date || 'after staff review'}.`,
    createdAt: item.created_at,
    read: false
  }));

  return res.json({
    notifications: [...overdueNotifications, ...reservationNotifications]
      .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
      .slice(0, 12)
  });
}));

router.post('/change-password', requireStudentSession, asyncRoute(async (req, res) => {
  const { currentPassword, newPassword } = req.body;
  const userId = req.session.user.id;

  if (!currentPassword || !newPassword) {
    return res.status(400).json({ message: 'Current and new password are required.' });
  }

  const passwordValidation = validatePasswordStrength(newPassword);
  if (!passwordValidation.valid) {
    return res.status(400).json({ message: passwordValidation.message });
  }

  const [rows] = await db.query('SELECT password_hash FROM users WHERE user_id = ? AND role IN (\'USER\', \'STUDENT\')', [userId]);
  const user = rows[0];
  if (!user || !verifyPassword(currentPassword, user.password_hash)) {
    return res.status(401).json({ message: 'Current password is incorrect.' });
  }

  const newHash = hashPassword(newPassword);
  await db.query('UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?', [newHash, userId]);
  return res.json({ message: 'Password changed successfully.' });
}));

router.use((err, req, res, next) => {
  console.error(err);
  return res.status(500).json({ message: 'A server error occurred. Please try again later.' });
});

module.exports = router;
