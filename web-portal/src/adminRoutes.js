const express = require('express');
const db = require('./db');
const { hashPassword } = require('./passwordHasher');

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

router.get('/equipment', requireAuth, requireAdmin, async (req, res) => {
  const [rows] = await db.query(
    'SELECT equipment_id, asset_tag, name, category, description, status, total_quantity, available_quantity FROM equipment ORDER BY name ASC'
  );
  return res.json({ equipment: rows });
});

router.post('/equipment', requireAuth, requireAdmin, async (req, res) => {
  const { name, assetTag, category, description, totalQuantity, status } = req.body;
  if (!name || !assetTag || !category || !totalQuantity) {
    return res.status(400).json({ message: 'Equipment name, asset tag, category, and quantity are required.' });
  }

  const parsedQuantity = parseInt(totalQuantity, 10);
  if (Number.isNaN(parsedQuantity) || parsedQuantity < 1) {
    return res.status(400).json({ message: 'Total quantity must be a positive number.' });
  }

  const validStatus = ['AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE'].includes(status)
    ? status
    : 'AVAILABLE';

  try {
    await db.query(
      'INSERT INTO equipment (asset_tag, name, category, description, status, total_quantity, available_quantity) VALUES (?, ?, ?, ?, ?, ?, ?)',
      [assetTag, name, category, description || null, validStatus, parsedQuantity, parsedQuantity]
    );
    return res.status(201).json({ message: 'Equipment created successfully.' });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: 'Failed to create equipment item.' });
  }
});

router.put('/equipment/:id', requireAuth, requireAdmin, async (req, res) => {
  const equipmentId = req.params.id;
  const { name, assetTag, category, description, totalQuantity, status } = req.body;

  const fields = [];
  const values = [];

  if (name) {
    fields.push('name = ?');
    values.push(name);
  }
  if (assetTag) {
    fields.push('asset_tag = ?');
    values.push(assetTag);
  }
  if (category) {
    fields.push('category = ?');
    values.push(category);
  }
  if (description !== undefined) {
    fields.push('description = ?');
    values.push(description || null);
  }
  if (status) {
    fields.push('status = ?');
    values.push(['AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE'].includes(status) ? status : 'AVAILABLE');
  }
  if (totalQuantity !== undefined) {
    const parsedQuantity = parseInt(totalQuantity, 10);
    if (Number.isNaN(parsedQuantity) || parsedQuantity < 1) {
      return res.status(400).json({ message: 'Total quantity must be a positive number.' });
    }
    fields.push('total_quantity = ?');
    values.push(parsedQuantity);
    fields.push('available_quantity = LEAST(available_quantity, ?)');
    values.push(parsedQuantity);
  }

  if (fields.length === 0) {
    return res.status(400).json({ message: 'No equipment fields provided for update.' });
  }

  values.push(equipmentId);

  try {
    const [result] = await db.query(`UPDATE equipment SET ${fields.join(', ')} WHERE equipment_id = ?`, values);
    if (result.affectedRows === 0) {
      return res.status(404).json({ message: 'Equipment item not found.' });
    }
    return res.json({ message: 'Equipment updated successfully.' });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: 'Failed to update equipment item.' });
  }
});

router.delete('/equipment/:id', requireAuth, requireAdmin, async (req, res) => {
  const equipmentId = req.params.id;
  try {
    const [result] = await db.query('DELETE FROM equipment WHERE equipment_id = ?', [equipmentId]);
    if (result.affectedRows === 0) {
      return res.status(404).json({ message: 'Equipment item not found.' });
    }
    return res.json({ message: 'Equipment deleted successfully.' });
  } catch (error) {
    console.error(error);
    return res.status(500).json({ message: 'Failed to delete equipment item.' });
  }
});

router.get('/users', requireAuth, requireAdmin, async (req, res) => {
  const [rows] = await db.query(
    `SELECT user_id, full_name, username, email, branch, course, block, year_level, phone_number, role, is_active
     FROM users
     WHERE role IN ('USER', 'STUDENT', 'ADMIN')
     ORDER BY full_name ASC`
  );
  const users = rows.map(row => ({
    ...row,
    role: row.role === 'STUDENT' ? 'USER' : row.role
  }));
  return res.json({ users });
});

router.post('/users', requireAuth, requireAdmin, async (req, res) => {
  const {
    firstName,
    middleName,
    lastName,
    suffix,
    userId,
    phoneNumber,
    department,
    course,
    yearLevel,
    block,
    password
  } = req.body;

  if (!firstName || !lastName || !userId || !phoneNumber || !department || !course || !yearLevel || !block || !password) {
    return res.status(400).json({ message: 'Missing required user fields.' });
  }

  if (!/^[0-9]+$/.test(userId)) {
    return res.status(400).json({ message: 'User ID must be numeric.' });
  }

  if (!/^[0-9]{11}$/.test(phoneNumber)) {
    return res.status(400).json({ message: 'Phone number must be 11 digits.' });
  }

  const fullName = [firstName.trim(), middleName?.trim(), lastName.trim(), suffix?.trim()]
    .filter(Boolean)
    .join(' ');
  const email = `${userId}@gordoncollege.edu.ph`;
  const passwordHash = hashPassword(password);

  try {
    await db.query(
      'INSERT INTO users (full_name, username, email, branch, course, block, year_level, phone_number, password_hash, role) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
      [fullName, userId, email, department, course, block, parseInt(yearLevel, 10), phoneNumber, passwordHash, 'USER']
    );
    return res.status(201).json({ message: 'User account created successfully.' });
  } catch (error) {
    if (error.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ message: 'A user with the same user ID or email already exists.' });
    }
    console.error(error);
    return res.status(500).json({ message: 'Failed to create user account.' });
  }
});

router.get('/approvals', requireAuth, requireAdmin, async (req, res) => {
  const [rows] = await db.query(
    `SELECT r.reservation_id, r.equipment_id, r.quantity, r.status, r.request_date, r.due_date, r.remarks,
            u.full_name AS user_name, u.username AS user_id,
            e.name AS equipment_name, e.asset_tag
     FROM reservations r
     JOIN users u ON r.user_id = u.user_id
     JOIN equipment e ON r.equipment_id = e.equipment_id
     WHERE r.status = 'PENDING'
     ORDER BY r.request_date ASC`
  );
  return res.json({ approvals: rows });
});

router.put('/approvals/:id', requireAuth, requireAdmin, async (req, res) => {
  const reservationId = req.params.id;
  const { action, dueDate } = req.body;

  if (!action || !['approve', 'decline'].includes(action)) {
    return res.status(400).json({ message: 'Action must be approve or decline.' });
  }

  const [rows] = await db.query(
    'SELECT reservation_id, equipment_id, quantity, status FROM reservations WHERE reservation_id = ?',
    [reservationId]
  );
  const reservation = rows[0];
  if (!reservation || reservation.status !== 'PENDING') {
    return res.status(404).json({ message: 'Pending reservation not found.' });
  }

  if (action === 'decline') {
    await db.query('UPDATE reservations SET status = ? WHERE reservation_id = ?', ['DECLINED', reservationId]);
    return res.json({ message: 'Reservation request declined.' });
  }

  const [equipmentRows] = await db.query(
    'SELECT available_quantity FROM equipment WHERE equipment_id = ?',
    [reservation.equipment_id]
  );
  const equipment = equipmentRows[0];
  if (!equipment || equipment.available_quantity < reservation.quantity) {
    return res.status(400).json({ message: 'Not enough equipment available to approve this request.' });
  }

  const approvedDueDate = dueDate || new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10);

  await db.query('UPDATE reservations SET status = ?, due_date = ? WHERE reservation_id = ?', ['APPROVED', approvedDueDate, reservationId]);
  await db.query('UPDATE equipment SET available_quantity = available_quantity - ? WHERE equipment_id = ?', [reservation.quantity, reservation.equipment_id]);

  return res.json({ message: 'Reservation approved successfully.' });
});

router.put('/returns/:id', requireAuth, requireAdmin, async (req, res) => {
  const reservationId = req.params.id;
  const [rows] = await db.query(
    'SELECT reservation_id, equipment_id, quantity, status, due_date, user_id FROM reservations WHERE reservation_id = ?',
    [reservationId]
  );
  const reservation = rows[0];
  if (!reservation || !['APPROVED', 'OVERDUE'].includes(reservation.status)) {
    return res.status(404).json({ message: 'Approved reservation not found for return processing.' });
  }

  const now = new Date();

  // determine if this return is late
  let daysLate = 0;
  if (reservation.due_date) {
    const due = new Date(reservation.due_date);
    if (now > due) {
      const diff = now - due;
      daysLate = Math.ceil(diff / (24 * 60 * 60 * 1000));
    }
  }

  // default penalty rule: 1 day late => 1 day penalty (can be adjusted later)
  const penaltyDays = daysLate > 0 ? daysLate : 0;
  const penaltyEndDate = penaltyDays > 0 ? new Date(now.getTime() + penaltyDays * 24 * 60 * 60 * 1000) : null;

  await db.query('UPDATE reservations SET status = ?, return_date = ?, returned_at = ?, is_late = ?, penalty_end_date = ? WHERE reservation_id = ?', ['RETURNED', now, now, daysLate > 0 ? 1 : 0, penaltyEndDate, reservationId]);
  await db.query('UPDATE equipment SET available_quantity = available_quantity + ? WHERE equipment_id = ?', [reservation.quantity, reservation.equipment_id]);

  // create an overdue record if late
  if (daysLate > 0) {
    await db.query(
      'INSERT INTO overdues (reservation_id, user_id, equipment_id, days_late, penalty_days, penalty_end_date) VALUES (?, ?, ?, ?, ?, ?)',
      [reservationId, reservation.user_id, reservation.equipment_id, daysLate, penaltyDays, penaltyEndDate]
    );
  }

  return res.json({ message: 'Return processed successfully.', late: daysLate > 0, daysLate });
});

// Admin: list all overdue incidents
router.get('/overdues', requireAuth, requireAdmin, async (req, res) => {
  const [rows] = await db.query(
    `SELECT o.overdue_id, o.reservation_id, o.user_id, u.full_name AS user_name, o.equipment_id, e.name AS equipment_name, e.asset_tag, o.days_late, o.penalty_days, o.penalty_end_date, o.settled, o.settled_at, o.created_at
     FROM overdues o
     JOIN users u ON o.user_id = u.user_id
     JOIN equipment e ON o.equipment_id = e.equipment_id
     ORDER BY o.created_at DESC`
  );
  return res.json({ overdues: rows });
});

// Admin: update overdue (e.g., mark settled)
router.put('/overdues/:id', requireAuth, requireAdmin, async (req, res) => {
  const overdueId = req.params.id;
  const { settled } = req.body;
  if (typeof settled === 'undefined') {
    return res.status(400).json({ message: 'Nothing to update.' });
  }
  if (settled) {
    await db.query('UPDATE overdues SET settled = ?, settled_at = ? WHERE overdue_id = ?', [1, new Date(), overdueId]);
  } else {
    await db.query('UPDATE overdues SET settled = ?, settled_at = NULL WHERE overdue_id = ?', [0, overdueId]);
  }
  return res.json({ message: 'Overdue updated.' });
});

module.exports = router;
