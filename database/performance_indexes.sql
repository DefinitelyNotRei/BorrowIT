-- Performance Optimization Indexes for BorrowIT
-- This file adds indexes to improve query performance
-- Run this after the initial schema setup

USE borrowit;

-- Composite indexes for common query patterns
-- These indexes significantly improve performance for reservation queries
CREATE INDEX IF NOT EXISTS idx_reservations_user_status_date ON reservations(user_id, status, request_date);
CREATE INDEX IF NOT EXISTS idx_reservations_equipment_status ON reservations(equipment_id, status);
CREATE INDEX IF NOT EXISTS idx_reservations_status_due_date ON reservations(status, due_date);

-- Search indexes for users table
-- Improves user search and authentication performance
CREATE INDEX IF NOT EXISTS idx_users_search ON users(full_name, username, email);
CREATE INDEX IF NOT EXISTS idx_users_active_role ON users(is_active, role);

-- Equipment search and filtering indexes
-- Improves equipment listing and availability checks
CREATE INDEX IF NOT EXISTS idx_equipment_category_status ON equipment(category, status);
CREATE INDEX IF NOT EXISTS idx_equipment_status_available ON equipment(status, available_quantity);

-- Overdue tracking indexes
-- Improves overdue queries and penalty checks
CREATE INDEX IF NOT EXISTS idx_overdues_user_settled ON overdues(user_id, settled);
CREATE INDEX IF NOT EXISTS idx_overdues_penalty_date ON overdues(penalty_end_date);

-- Admin lookup indexes
-- Improves admin-related queries
CREATE INDEX IF NOT EXISTS idx_admins_active ON admins(is_active, user_id);

-- Full-text search indexes (MySQL 5.6+)
-- Improves text search performance
-- Note: These require InnoDB tables and MySQL 5.6+
ALTER TABLE equipment ADD FULLTEXT INDEX ft_equipment_search (name, description);
ALTER TABLE users ADD FULLTEXT INDEX ft_users_search (full_name);

-- Verify indexes were created
SHOW INDEX FROM reservations;
SHOW INDEX FROM users;
SHOW INDEX FROM equipment;
SHOW INDEX FROM overdues;
