-- Stored function, stored procedure and triggers.
-- The app runs this file at startup. You can also run it by hand in MySQL Workbench / mysql client.

USE eventdb;

DELIMITER //

-- FUNCTION: number of registrations for an event
DROP FUNCTION IF EXISTS fn_event_registration_count
//
CREATE FUNCTION fn_event_registration_count(p_event_id INT)
RETURNS INT
READS SQL DATA
BEGIN
    DECLARE v_count INT;
    SELECT COUNT(*) INTO v_count FROM registrations WHERE event_id = p_event_id;
    RETURN v_count;
END
//

-- PROCEDURE: register a participant for an event (checks event exists, not expired, seats, duplicates)
DROP PROCEDURE IF EXISTS sp_register_participant
//
CREATE PROCEDURE sp_register_participant(
    IN  p_event_id       INT,
    IN  p_participant_id INT,
    OUT p_success        TINYINT,
    OUT p_message        VARCHAR(120))
BEGIN
    DECLARE v_seats INT DEFAULT NULL;
    DECLARE v_date  DATE DEFAULT NULL;

    SET p_success = 0;
    SELECT available_seats, event_date INTO v_seats, v_date FROM events WHERE event_id = p_event_id;

    IF v_seats IS NULL THEN
        SET p_message = 'Event not found';
    ELSEIF v_date < CURDATE() THEN
        SET p_message = 'Event expired - registration is closed';
    ELSEIF (SELECT COUNT(*) FROM participants WHERE participant_id = p_participant_id) = 0 THEN
        SET p_message = 'Participant not found';
    ELSEIF (SELECT COUNT(*) FROM registrations
            WHERE event_id = p_event_id AND participant_id = p_participant_id) > 0 THEN
        SET p_message = 'Participant is already registered for this event';
    ELSEIF v_seats <= 0 THEN
        SET p_message = 'No seats available for this event';
    ELSE
        INSERT INTO registrations (event_id, participant_id) VALUES (p_event_id, p_participant_id);
        SET p_success = 1;
        SET p_message = 'Registration successful';
    END IF;
END
//

-- TRIGGER 1: refuse a registration when the event is expired or full (safety net)
DROP TRIGGER IF EXISTS trg_registration_before_insert
//
CREATE TRIGGER trg_registration_before_insert
BEFORE INSERT ON registrations
FOR EACH ROW
BEGIN
    DECLARE v_seats INT DEFAULT NULL;
    DECLARE v_date  DATE DEFAULT NULL;
    SELECT available_seats, event_date INTO v_seats, v_date FROM events WHERE event_id = NEW.event_id;
    IF v_date < CURDATE() THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Event expired - registration is closed';
    END IF;
    IF v_seats IS NULL OR v_seats <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No seats available for this event';
    END IF;
END
//

-- TRIGGER 2: update available seats after a registration
DROP TRIGGER IF EXISTS trg_registration_after_insert
//
CREATE TRIGGER trg_registration_after_insert
AFTER INSERT ON registrations
FOR EACH ROW
BEGIN
    UPDATE events SET available_seats = available_seats - 1 WHERE event_id = NEW.event_id;
END
//

-- TRIGGER 3: give the seat back when a registration is cancelled
DROP TRIGGER IF EXISTS trg_registration_after_delete
//
CREATE TRIGGER trg_registration_after_delete
AFTER DELETE ON registrations
FOR EACH ROW
BEGIN
    UPDATE events SET available_seats = available_seats + 1 WHERE event_id = OLD.event_id;
END
//

DELIMITER ;
