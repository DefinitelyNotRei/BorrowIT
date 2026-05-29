DROP DATABASE IF EXISTS borrowit;

CREATE DATABASE IF NOT EXISTS borrowit
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE borrowit;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    branch VARCHAR(100) NOT NULL DEFAULT 'General',
    course VARCHAR(100) NOT NULL DEFAULT 'General',
    block CHAR(1) NOT NULL DEFAULT 'A',
    year_level INT NOT NULL DEFAULT 1,
    phone_number VARCHAR(20) NOT NULL DEFAULT '',
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('USER', 'STUDENT', 'ADMIN') NOT NULL DEFAULT 'USER',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    account_status ENUM('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    email_verified_at TIMESTAMP NULL,
    verification_token_hash VARCHAR(128) NULL,
    verification_sent_at DATETIME NULL,
    password_reset_token_hash VARCHAR(128) NULL,
    password_reset_expires_at DATETIME NULL,
    profile_image_url VARCHAR(500) NULL,
    last_login_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_users_username_length CHECK (CHAR_LENGTH(username) >= 3),
    CONSTRAINT chk_users_year_level CHECK (year_level >= 1 AND year_level <= 4)
);

CREATE TABLE IF NOT EXISTS admins (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    staff_code VARCHAR(50) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL DEFAULT 'IT Services',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_admins_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS equipment (
    equipment_id INT AUTO_INCREMENT PRIMARY KEY,
    asset_tag VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    category VARCHAR(100) NOT NULL DEFAULT 'General',
    description VARCHAR(500),
    location VARCHAR(120) NULL,
    image_url VARCHAR(500) NULL,
    status ENUM('AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE', 'RETIRED') NOT NULL DEFAULT 'AVAILABLE',
    total_quantity INT NOT NULL DEFAULT 1,
    available_quantity INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_equipment_total_quantity CHECK (total_quantity >= 0),
    CONSTRAINT chk_equipment_available_quantity CHECK (available_quantity >= 0 AND available_quantity <= total_quantity)
);

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    equipment_id INT NOT NULL,
    reference_number VARCHAR(40) NULL UNIQUE,
    quantity INT NOT NULL DEFAULT 1,
    status ENUM('PENDING', 'APPROVED', 'DECLINED', 'RETURNED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    expires_at DATETIME NULL,
    due_date DATETIME NULL,
    return_date DATETIME NULL,
    returned_at TIMESTAMP NULL,
    processed_by_admin_id INT NULL,
    is_late TINYINT(1) NOT NULL DEFAULT 0,
    penalty_end_date DATETIME NULL,
    remarks VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_reservations_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_reservations_equipment
        FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_reservations_processed_by_admin
        FOREIGN KEY (processed_by_admin_id) REFERENCES admins(admin_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT chk_reservations_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_account_status ON users(account_status);
CREATE INDEX idx_users_reset_token ON users(password_reset_token_hash);
CREATE INDEX idx_equipment_status ON equipment(status);
CREATE INDEX idx_equipment_name ON equipment(name);
CREATE INDEX idx_equipment_category_status ON equipment(category, status);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_user_status ON reservations(user_id, status);
CREATE INDEX idx_reservations_equipment_status ON reservations(equipment_id, status);
CREATE INDEX idx_reservations_due_date ON reservations(due_date);
CREATE INDEX idx_reservations_reference_number ON reservations(reference_number);
CREATE INDEX idx_reservations_expires_at ON reservations(expires_at);

-- Track overdue incidents separately so they remain visible after return
CREATE TABLE IF NOT EXISTS overdues (
    overdue_id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL,
    user_id INT NOT NULL,
    equipment_id INT NOT NULL,
    days_late INT NOT NULL DEFAULT 0,
    penalty_days INT NOT NULL DEFAULT 0,
    penalty_end_date DATETIME NULL,
    settled TINYINT(1) NOT NULL DEFAULT 0,
    settled_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_overdues_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE CASCADE,
    CONSTRAINT fk_overdues_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_overdues_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id) ON DELETE RESTRICT
);

CREATE INDEX idx_overdues_user ON overdues(user_id);
CREATE INDEX idx_overdues_settled ON overdues(settled);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    type ENUM('APPROVAL', 'DECLINE', 'DUE_REMINDER', 'OVERDUE', 'RESERVATION_UPDATE', 'SYSTEM') NOT NULL DEFAULT 'SYSTEM',
    title VARCHAR(160) NOT NULL,
    message VARCHAR(700) NOT NULL,
    read_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE INDEX idx_notifications_user_read ON notifications(user_id, read_at, created_at);

CREATE TABLE IF NOT EXISTS activity_logs (
    activity_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_user_id INT NULL,
    actor_role VARCHAR(30) NOT NULL DEFAULT 'SYSTEM',
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id INT NULL,
    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(255) NULL,
    metadata JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_logs_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE INDEX idx_activity_logs_entity ON activity_logs(entity_type, entity_id);
CREATE INDEX idx_activity_logs_actor_date ON activity_logs(actor_user_id, created_at);

CREATE TABLE IF NOT EXISTS reservation_status_history (
    history_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reservation_id INT NOT NULL,
    from_status VARCHAR(30) NULL,
    to_status VARCHAR(30) NOT NULL,
    changed_by_user_id INT NULL,
    remarks VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_status_history_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_status_history_user
        FOREIGN KEY (changed_by_user_id) REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE INDEX idx_status_history_reservation ON reservation_status_history(reservation_id, created_at);
