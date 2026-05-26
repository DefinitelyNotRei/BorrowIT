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
    quantity INT NOT NULL DEFAULT 1,
    status ENUM('PENDING', 'APPROVED', 'DECLINED', 'RETURNED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    request_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP NULL,
    due_date DATETIME NULL,
    return_date DATETIME NULL,
    returned_at TIMESTAMP NULL,
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
    CONSTRAINT chk_reservations_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_equipment_status ON equipment(status);
CREATE INDEX idx_equipment_name ON equipment(name);
CREATE INDEX idx_reservations_status ON reservations(status);
CREATE INDEX idx_reservations_user_status ON reservations(user_id, status);
CREATE INDEX idx_reservations_equipment_status ON reservations(equipment_id, status);
CREATE INDEX idx_reservations_due_date ON reservations(due_date);
