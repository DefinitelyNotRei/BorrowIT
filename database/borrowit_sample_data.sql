-- Additional sample data for BorrowIT database
-- This file contains more sample users, equipment, and reservations
-- Run this after the initial seed data

USE borrowit;



-- Additional Equipment
INSERT INTO equipment (equipment_id, asset_tag, name, category, description, status, total_quantity, available_quantity)
VALUES
    (1, 'SPORT-001', 'Baseball Glove Set', 'Baseball', 'Professional baseball gloves for all positions.', 'AVAILABLE', 8, 8),
    (2, 'SPORT-002', 'Basketball Premium', 'Basketball', 'Official size basketball for competitions and training.', 'AVAILABLE', 5, 5),
    (3, 'SPORT-003', 'Volleyball Set', 'Volleyball', 'Professional volleyball with mesh carrying bag.', 'AVAILABLE', 10, 10),
    (4, 'SPORT-004', 'Soccer Ball Pack', 'Soccer', 'High-quality soccer balls (size 5) for matches.', 'AVAILABLE', 6, 6),
    (5, 'SPORT-005', 'Tennis Racket Set', 'Tennis', 'Professional tennis rackets with carrying cases.', 'AVAILABLE', 15, 15),
    (6, 'SPORT-006', 'Badminton Set', 'Badminton', 'Complete badminton set with net, rackets, and shuttlecocks.', 'AVAILABLE', 12, 12),
    (7, 'SPORT-007', 'Cricket Bat Pack', 'Cricket', 'Wooden cricket bats for training and matches.', 'AVAILABLE', 10, 10),
    (8, 'SPORT-008', 'Handball', 'Handball', 'Professional handballs for sports training.', 'AVAILABLE', 4, 4),
    (9, 'SPORT-009', 'Ping Pong Set', 'Table Tennis', 'Table tennis paddles and balls set.', 'AVAILABLE', 7, 7),
    (10, 'SPORT-010', 'Swimming Equipment', 'Swimming', 'Kickboards, flippers, and pull buoys for pool training.', 'AVAILABLE', 9, 9),
    (11, 'SPORT-014', 'Boxing Gloves', 'Boxing', 'Professional boxing gloves for training and sparring.', 'AVAILABLE', 4, 4),
    (12, 'SPORT-015', 'Football', 'Football', 'American football official size and weight.', 'AVAILABLE', 2, 2)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    description = VALUES(description),
    status = VALUES(status),
    total_quantity = VALUES(total_quantity),
    available_quantity = VALUES(available_quantity);