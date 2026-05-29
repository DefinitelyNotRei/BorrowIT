USE borrowit;

DELIMITER //

DROP PROCEDURE IF EXISTS add_column_if_missing//
CREATE PROCEDURE add_column_if_missing(
    IN table_name_param VARCHAR(64),
    IN column_name_param VARCHAR(64),
    IN column_definition_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND COLUMN_NAME = column_name_param
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', table_name_param, ' ADD COLUMN ', column_definition_param);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS add_index_if_missing//
CREATE PROCEDURE add_index_if_missing(
    IN table_name_param VARCHAR(64),
    IN index_name_param VARCHAR(64),
    IN index_definition_param TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM INFORMATION_SCHEMA.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_param
          AND INDEX_NAME = index_name_param
    ) THEN
        SET @ddl = CONCAT('CREATE INDEX ', index_name_param, ' ON ', table_name_param, ' ', index_definition_param);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL add_column_if_missing('users', 'account_status', "account_status ENUM('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE' AFTER is_active");
CALL add_column_if_missing('users', 'email_verified_at', 'email_verified_at TIMESTAMP NULL AFTER account_status');
CALL add_column_if_missing('users', 'verification_token_hash', 'verification_token_hash VARCHAR(128) NULL AFTER email_verified_at');
CALL add_column_if_missing('users', 'verification_sent_at', 'verification_sent_at DATETIME NULL AFTER verification_token_hash');
CALL add_column_if_missing('users', 'password_reset_token_hash', 'password_reset_token_hash VARCHAR(128) NULL AFTER verification_sent_at');
CALL add_column_if_missing('users', 'password_reset_expires_at', 'password_reset_expires_at DATETIME NULL AFTER password_reset_token_hash');
CALL add_column_if_missing('users', 'profile_image_url', 'profile_image_url VARCHAR(500) NULL AFTER password_reset_expires_at');
CALL add_column_if_missing('users', 'last_login_at', 'last_login_at DATETIME NULL AFTER profile_image_url');

CALL add_column_if_missing('equipment', 'location', 'location VARCHAR(120) NULL AFTER description');
CALL add_column_if_missing('equipment', 'image_url', 'image_url VARCHAR(500) NULL AFTER location');

CALL add_column_if_missing('reservations', 'reference_number', 'reference_number VARCHAR(40) NULL UNIQUE AFTER equipment_id');
CALL add_column_if_missing('reservations', 'expires_at', 'expires_at DATETIME NULL AFTER approved_at');
CALL add_column_if_missing('reservations', 'processed_by_admin_id', 'processed_by_admin_id INT NULL AFTER returned_at');

UPDATE users
SET account_status = 'ACTIVE'
WHERE account_status IS NULL;

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

CALL add_index_if_missing('users', 'idx_users_account_status', '(account_status)');
CALL add_index_if_missing('users', 'idx_users_reset_token', '(password_reset_token_hash)');
CALL add_index_if_missing('equipment', 'idx_equipment_category_status', '(category, status)');
CALL add_index_if_missing('reservations', 'idx_reservations_reference_number', '(reference_number)');
CALL add_index_if_missing('reservations', 'idx_reservations_expires_at', '(expires_at)');
CALL add_index_if_missing('notifications', 'idx_notifications_user_read', '(user_id, read_at, created_at)');
CALL add_index_if_missing('activity_logs', 'idx_activity_logs_entity', '(entity_type, entity_id)');
CALL add_index_if_missing('activity_logs', 'idx_activity_logs_actor_date', '(actor_user_id, created_at)');
CALL add_index_if_missing('reservation_status_history', 'idx_status_history_reservation', '(reservation_id, created_at)');

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS add_index_if_missing;
