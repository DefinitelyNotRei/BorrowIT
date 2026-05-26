const express = require('express');
const db = require('./db');
const { hashPassword, verifyPassword } = require('./passwordHasher');

const router = express.Router();

function requireAuth(req, res, next) {
  if (req.session?.user?.id) {
    return next();
  }
  return res.status(401).json({ message: 'Authentication required.' });
}

router.post('/login', async (req, res) => {
  const { username, password } = req.body;

  if (!username || !password) {
    return res.status(400).json({ message: 'Username and password are required.' });
  }

  const [rows] = await db.query(
    'SELECT user_id, full_name, username, email, branch, course, block, year_level, phone_number, role, is_active, password_hash FROM users WHERE username = ?',
    [username]
  );

  const user = rows[0];
  if (!user || !user.is_active || !['USER', 'STUDENT'].includes(user.role)) {
    return res.status(401).json({ message: 'Invalid credentials.' });
  }

  if (!verifyPassword(password, user.password_hash)) {
    return res.status(401).json({ message: 'Invalid credentials.' });
  }

  req.session.user = {
    id: user.user_id,
    fullName: user.full_name,
    username: user.username,
    email: user.email,
    role: user.role
  };

  return res.json({
    id: user.user_id,
    fullName: user.full_name,
    username: user.username,
    email: user.email,
    role: user.role
  });
});

router.post('/register', async (req, res) => {
  const {
    firstName,
    middleName,
    lastName,
    suffix,
    studentId,
    phoneNumber,
    branch,
    course,
    yearLevel,
    block,
    password
  } = req.body;

  if (!firstName || !lastName || !studentId || !phoneNumber || !branch || !course || !yearLevel || !block || !password) {
    return res.status(400).json({ message: 'Missing required registration fields.' });
  }

  if (!/^[0-9]+$/.test(studentId)) {
    return res.status(400).json({ message: 'Student ID must be numeric.' });
  }

  if (!/^[0-9]{11}$/.test(phoneNumber)) {
    return res.status(400).json({ message: 'Phone number must be 11 digits.' });
  }

  const fullName = [firstName.trim(), middleName?.trim(), lastName.trim(), suffix?.trim()]
    .filter(Boolean)
    .join(' ');
  const email = `${studentId}@gordoncollege.edu.ph`;
  const passwordHash = hashPassword(password);

  try {
    await db.query(
      'INSERT INTO users (full_name, username, email, branch, course, block, year_level, phone_number, password_hash, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
      [fullName, studentId, email, branch, course, block, parseInt(yearLevel, 10), phoneNumber, passwordHash, 'STUDENT']
    );

    return res.status(201).json({ message: 'Registration complete. Please login.' });
  } catch (error) {
    if (error.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ message: 'A user with the same student ID or email already exists.' });
    }
    console.error(error);
    return res.status(500).json({ message: 'An error occurred while registering.' });
  }
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
  const where = ['status = ?', 'available_quantity > 0'];
  const params = ['AVAILABLE'];

  if (search) {
    where.push('(name LIKE ? OR asset_tag LIKE ? OR description LIKE ?)');
    params.push(`%${search}%`, `%${search}%`, `%${search}%`);
  }

  const [rows] = await db.query(
    `SELECT equipment_id, asset_tag, name, category, description, status, total_quantity, available_quantity FROM equipment WHERE ${where.join(' AND ')} ORDER BY name ASC`,
    params
  );
  return res.json({ equipment: rows });
});

router.post('/reservations', requireAuth, async (req, res) => {
  const { equipmentId, quantity = 1, remarks } = req.body;
  const userId = req.session.user.id;

  if (!equipmentId) {
    return res.status(400).json({ message: 'Equipment id is required.' });
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
