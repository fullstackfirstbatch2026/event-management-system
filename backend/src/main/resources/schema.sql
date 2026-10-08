-- Tables for the Event Registration System

CREATE TABLE IF NOT EXISTS organizers (
    organizer_id INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    email        VARCHAR(120) NOT NULL UNIQUE,
    phone        VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS events (
    event_id        INT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(150) NOT NULL,
    description     VARCHAR(500),
    event_date      DATE NOT NULL,
    venue           VARCHAR(150) NOT NULL,
    total_seats     INT NOT NULL CHECK (total_seats > 0),
    available_seats INT NOT NULL CHECK (available_seats >= 0),
    organizer_id    INT NOT NULL,
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES organizers (organizer_id)
);

CREATE TABLE IF NOT EXISTS participants (
    participant_id INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    email          VARCHAR(120) NOT NULL UNIQUE,
    phone          VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS registrations (
    registration_id   INT AUTO_INCREMENT PRIMARY KEY,
    event_id          INT NOT NULL,
    participant_id    INT NOT NULL,
    registration_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_registration UNIQUE (event_id, participant_id),
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES events (event_id),
    CONSTRAINT fk_reg_participant FOREIGN KEY (participant_id) REFERENCES participants (participant_id)
);
