USE borrowit;

INSERT INTO users (user_id, full_name, username, email, year_level, phone_number,  password_hash, role, is_active)
VALUES
    (1, 'System Administrator', 'admin', 'admin@gordoncollege.edu.ph', 1, '0912-345-6789', '65536:ABEiM0RVZneImaq7zN3u/w==:+i/JERJAnnWwPa09aFO5oYFUzo8bQwIUmd1YE19uEDU=', 'ADMIN', 1)
ON DUPLICATE KEY UPDATE
    full_name = VALUES(full_name),
    email = VALUES(email),
    year_level = VALUES(year_level),
    phone_number = VALUES(phone_number),
    password_hash = VALUES(password_hash),
    role = VALUES(role),
    is_active = VALUES(is_active);

INSERT INTO admins (admin_id, user_id, staff_code, department, is_active)
VALUES
    (1, 1, 'ADM-0001', 'IT Services', 1)
ON DUPLICATE KEY UPDATE
    staff_code = VALUES(staff_code),
    department = VALUES(department),
    is_active = VALUES(is_active);

