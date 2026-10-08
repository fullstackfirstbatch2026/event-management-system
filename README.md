# Event Registration System

Spring Boot REST API + MySQL + plain HTML/CSS/JS front end.

```
event-app/
├── backend/    Spring Boot 3 (Java 17, Maven, JdbcTemplate)
├── frontend/   index.html, style.css, script.js
└── database/   schema.sql, routines.sql (copies of the files the backend runs)
```

## Run
1. Edit `backend/src/main/resources/application.properties` and set your MySQL username and password.
2. `cd backend && mvn spring-boot:run`
   - creates database `eventdb`, the 4 tables, the function, procedure and triggers
   - adds sample data when the database is empty
3. Open `frontend/index.html` in a browser.

## Requirements and where they are

| Requirement | Where |
|---|---|
| Tables EVENTS, ORGANIZERS, PARTICIPANTS, REGISTRATIONS | `schema.sql` |
| JOIN: registrations with event and participant details | `RegistrationRepository.SELECT_REGISTRATIONS` → `GET /api/registrations` |
| Subquery: events with above-average registrations | `EventRepository.findAboveAverageRegistrations` → `GET /api/events/above-average-registrations` |
| Procedure: register participant | `sp_register_participant` in `routines.sql` → `POST /api/registrations` |
| Function: event registration count | `fn_event_registration_count` in `routines.sql` → `GET /api/events/{id}/registration-count` |
| Trigger: update available seats | `trg_registration_after_insert` (seat −1), `trg_registration_after_delete` (seat +1), `trg_registration_before_insert` (blocks full events) |

## Date rules
- The event date picker does not allow past dates (and the API rejects them with "Event date cannot be in the past").
- Registering for an event whose date has passed shows "Event expired - registration is closed". The check lives in `sp_register_participant` and, as a safety net, in `trg_registration_before_insert`. Expired events are marked with an "Expired" badge.

## Endpoints
| Method | URL |
|---|---|
| GET, POST | /api/organizers |
| DELETE | /api/organizers/{id} |
| GET, POST | /api/participants |
| DELETE | /api/participants/{id} |
| GET, POST | /api/events |
| GET, DELETE | /api/events/{id} |
| GET | /api/events/above-average-registrations |
| GET | /api/events/{id}/registration-count |
| GET (optional ?eventId=), POST | /api/registrations |
| DELETE | /api/registrations/{id} (cancels, seat returns) |

## Postman examples
POST /api/organizers
```json
{ "name": "Tech Club", "email": "tech@example.com", "phone": "9840000001" }
```
POST /api/participants
```json
{ "name": "Arun Kumar", "email": "arun@example.com", "phone": "9841111111" }
```
POST /api/events
```json
{ "title": "Java Workshop", "description": "Spring Boot basics", "eventDate": "2026-11-15", "venue": "Lab 2", "totalSeats": 30, "organizerId": 1 }
```
POST /api/registrations
```json
{ "eventId": 1, "participantId": 1 }
```

## Running the SQL by hand (MySQL Workbench)
Run `schema.sql`, then `routines.sql` (it already contains the `DELIMITER` lines), then try:
```sql
CALL sp_register_participant(1, 2, @ok, @msg);
SELECT @ok, @msg;
SELECT fn_event_registration_count(1);
```
