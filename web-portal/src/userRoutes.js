const express = require('express');
const db = require('./db');
const { hashPassword, verifyPassword, validatePasswordStrength } = require('./passwordHasher');
const { recordFailedAttempt, isLockedOut, clearFailedAttempts, getRemainingAttempts } = require('./authLimiter');

const router = express.Router();

function requireAuth(req, res, next) {
  if (req.session?.user?.id) {
    return next();
  }
  return res.status(401).json({ message: 'Authentication required.' });
}

function requireAdmin(req, res, next) {
  if (req.session?.user?.role === 'ADMIN') {
    return next();
  }
  return res.status(403).json({ message: 'Admin access required.' });
}

router.post('/login', async (req, res) => {
  const { username, password } = req.body;
  const ip = req.ip || req.connection.remoteAddress;

  if (!username || !password) {
    return res.status(400).json({ message: 'Username and password are required.' });
  }

  // Check if account is locked out
  const lockoutStatus = isLockedOut(ip, username);
  if (lockoutStatus) {
    return res.status(429).json({ 
      message: `Account locked due to too many failed attempts. Please try again in ${lockoutStatus.remainingTime} minutes.`,
      locked: true,
      remainingTime: lockoutStatus.remainingTime
    });
  }

  const [rows] = await db.query(
    'SELECT user_id, full_name, username, email, branch, course, block, year_level, phone_number, role, is_active, password_hash FROM users WHERE username = ?',
    [username]
  );

  const user = rows[0];
  const dbRole = String(user?.role || '').toUpperCase();
  if (!user || !user.is_active || !['USER', 'STUDENT', 'ADMIN'].includes(dbRole)) {
    recordFailedAttempt(ip, username);
    const remaining = getRemainingAttempts(ip, username);
    return res.status(401).json({ 
      message: 'Invalid credentials.',
      remainingAttempts: remaining
    });
  }

  if (!verifyPassword(password, user.password_hash)) {
    recordFailedAttempt(ip, username);
    const remaining = getRemainingAttempts(ip, username);
    return res.status(401).json({ 
      message: 'Invalid credentials.',
      remainingAttempts: remaining
    });
  }

  // Clear failed attempts on successful login
  clearFailedAttempts(ip, username);

  if (!req.app.get('allowAdminLogin') && dbRole === 'ADMIN') {
    return res.status(401).json({ message: 'Invalid credentials.' });
  }

  const normalizedRole = dbRole === 'STUDENT' ? 'USER' : dbRole;

  req.session.user = {
    id: user.user_id,
    fullName: user.full_name,
    username: user.username,
    email: user.email,
    role: normalizedRole
  };

  return res.json({
    id: user.user_id,
    fullName: user.full_name,
    username: user.username,
    email: user.email,
    role: normalizedRole
  });
});

// Explicitly disable the public registration API.
router.post('/register', (req, res) => {
  return res.status(404).json({ message: 'Public registration is disabled.' });
});

router.post('/logout', (req, res) => {
  req.session.destroy(err => {
    if (err) {
      return res.status(500).json({ message: 'Unable to logout.' });
    }
    res.clearCookie('connect.sid');
    return res.json({ message: 'Logged out.' });
  });
});

router.get('/user', (req, res) => {
  if (!req.session?.user) {
    return res.status(200).json({ user: null });
  }
  return res.json({ user: req.session.user });
});

router.get('/equipment', requireAuth, async (req, res) => {
  const search = req.query.search?.trim();
  const page = parseInt(req.query.page) || 1;
  const limit = parseInt(req.query.limit) || 20;
  const offset = (page - 1) * limit;

  const where = ['status = ?', 'available_quantity > 0'];
  const params = ['AVAILABLE'];

  if (search) {
    where.push('(name LIKE ? OR asset_tag LIKE ? OR description LIKE ?)');
    params.push(`%${search}%`, `%${search}%`, `%${search}%`);
  }

  // Get total count for pagination
  const [countResult] = await db.query(
    `SELECT COUNT(*) as total FROM equipment WHERE ${where.join(' AND ')}`,
    params
  );
  const total = countResult[0].total;

  // Get paginated results
  const [rows] = await db.query(
    `SELECT equipment_id, asset_tag, name, category, description, status, total_quantity, available_quantity FROM equipment WHERE ${where.join(' AND ')} ORDER BY name ASC LIMIT ? OFFSET ?`,
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
});

router.post('/reservations', requireAuth, async (req, res) => {
  const { equipmentId, quantity = 1, remarks } = req.body;
  const userId = req.session.user.id;

  if (!equipmentId) {
    return res.status(400).json({ message: 'Equipment id is required.' });
  }

  // Check for active penalties/overdues that block new reservations
  const [blocking] = await db.query(
    'SELECT overdue_id, penalty_end_date FROM overdues WHERE user_id = ? AND settled = 0 AND penalty_end_date IS NOT NULL AND penalty_end_date > NOW() LIMIT 1',
    [userId]
  );
  if (blocking && blocking.length > 0) {
    const block = blocking[0];
    return res.status(403).json({ message: 'You have an active penalty. You cannot request new reservations until ' + new Date(block.penalty_end_date).toLocaleString(), blockedUntil: block.penalty_end_date });
  }

  const [equipmentRows] = await db.query('SELECT equipment_id, available_quantity, status FROM equipment WHERE equipment_id = ?', [equipmentId]);
  const equipment = equipmentRows[0];

  if (!equipment || equipment.status !== 'AVAILABLE' || equipment.available_quantity < quantity) {
    return res.status(400).json({ message: 'Selected equipment is not available.' });
  }

  await db.query(
    'INSERT INTO reservations (user_id, equipment_id, quantity, status, remarks) VALUES (?, ?, ?, ?, ?)',
    [userId, equipmentId, parseInt(quantity, 10), 'PENDING', remarks || null]
  );

  return res.status(201).json({ message: 'Reservation request submitted.' });
});

router.get('/reservations/current', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const [rows] = await db.query(
    `SELECT r.reservation_id, r.equipment_id, r.quantity, r.status, r.request_date, r.due_date, r.return_date, e.name, e.asset_tag
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ? AND r.status = 'APPROVED'
     ORDER BY r.request_date DESC`,
    [userId]
  );

  return res.json({ reservations: rows });
});

router.get('/reservations/history', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const [rows] = await db.query(
    `SELECT r.reservation_id, r.equipment_id, r.quantity, r.status, r.request_date, r.due_date, r.return_date, r.approved_at, r.remarks, e.name, e.asset_tag
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ?
     ORDER BY r.request_date DESC`,
    [userId]
  );

  return res.json({ reservations: rows });
});

router.get('/reservations/pending', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const [rows] = await db.query(
    `SELECT r.reservation_id, r.equipment_id, r.quantity, r.status, r.request_date, r.remarks, e.name, e.asset_tag
     FROM reservations r
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.user_id = ? AND r.status = 'PENDING'
     ORDER BY r.request_date DESC`,
    [userId]
  );

  return res.json({ reservations: rows });
});

// User: list overdue incidents for the current user
router.get('/overdues', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const [rows] = await db.query(
    `SELECT overdue_id, reservation_id, equipment_id, days_late, penalty_days, penalty_end_date, settled, settled_at, created_at
     FROM overdues
     WHERE user_id = ?
     ORDER BY created_at DESC`,
    [userId]
  );
  return res.json({ overdues: rows });
});

router.delete('/reservations/:id', requireAuth, async (req, res) => {
  const userId = req.session.user.id;
  const reservationId = req.params.id;

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

  await db.query('UPDATE reservations SET status = ? WHERE reservation_id = ?', ['CANCELLED', reservationId]);
  return res.json({ message: 'Reservation canceled.' });
});

router.post('/change-password', requireAuth, async (req, res) => {
  const { currentPassword, newPassword } = req.body;
  const userId = req.session.user.id;

  if (!currentPassword || !newPassword) {
    return res.status(400).json({ message: 'Current and new password are required.' });
  }

  // Validate new password strength
  const passwordValidation = validatePasswordStrength(newPassword);
  if (!passwordValidation.valid) {
    return res.status(400).json({ message: passwordValidation.message });
  }

  const [rows] = await db.query('SELECT password_hash FROM users WHERE user_id = ?', [userId]);
  const user = rows[0];
  if (!user || !verifyPassword(currentPassword, user.password_hash)) {
    return res.status(401).json({ message: 'Current password is incorrect.' });
  }

  const newHash = hashPassword(newPassword);
  await db.query('UPDATE users SET password_hash = ? WHERE user_id = ?', [newHash, userId]);
  return res.json({ message: 'Password changed successfully.' });
});

module.exports = router;
