package com.eventreg.repository;

import com.eventreg.model.EventRequest;
import com.eventreg.model.EventView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class EventRepository {

    private final JdbcTemplate jdbc;

    // fn_event_registration_count() is the stored function defined in routines.sql
    private static final String SELECT_EVENTS = """
            SELECT e.event_id, e.title, e.description, e.event_date, e.venue,
                   e.total_seats, e.available_seats, e.organizer_id,
                   o.name AS organizer_name,
                   fn_event_registration_count(e.event_id) AS registration_count
            FROM events e
            JOIN organizers o ON o.organizer_id = e.organizer_id
            """;

    private static final RowMapper<EventView> MAPPER = (rs, i) -> new EventView(
            rs.getLong("event_id"),
            rs.getString("title"),
            rs.getString("description"),
            rs.getObject("event_date", LocalDate.class),
            rs.getString("venue"),
            rs.getInt("total_seats"),
            rs.getInt("available_seats"),
            rs.getLong("organizer_id"),
            rs.getString("organizer_name"),
            rs.getInt("registration_count"));

    public EventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<EventView> findAll() {
        return jdbc.query(SELECT_EVENTS + " ORDER BY e.event_date, e.title", MAPPER);
    }

    public Optional<EventView> findById(long id) {
        return jdbc.query(SELECT_EVENTS + " WHERE e.event_id = ?", MAPPER, id).stream().findFirst();
    }

    /**
     * SUBQUERY: events whose registration count is above the average registrations per event.
     * The inner query counts registrations for every event (events with none count as 0).
     */
    public List<EventView> findAboveAverageRegistrations() {
        return jdbc.query(SELECT_EVENTS + """
                WHERE fn_event_registration_count(e.event_id) > (
                    SELECT AVG(t.cnt)
                    FROM (SELECT COUNT(r.registration_id) AS cnt
                          FROM events e2
                          LEFT JOIN registrations r ON r.event_id = e2.event_id
                          GROUP BY e2.event_id) t
                )
                ORDER BY registration_count DESC, e.title
                """, MAPPER);
    }

    public int registrationCount(long eventId) {
        Integer count = jdbc.queryForObject("SELECT fn_event_registration_count(?)", Integer.class, eventId);
        return count == null ? 0 : count;
    }

    public long create(EventRequest r) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO events (title, description, event_date, venue, total_seats, available_seats, organizer_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, r.title());
            ps.setString(2, r.description());
            ps.setDate(3, Date.valueOf(r.eventDate()));
            ps.setString(4, r.venue());
            ps.setInt(5, r.totalSeats());
            ps.setInt(6, r.totalSeats());          // a new event starts with every seat free
            ps.setLong(7, r.organizerId());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM events WHERE event_id = ?", id) > 0;
    }
}
