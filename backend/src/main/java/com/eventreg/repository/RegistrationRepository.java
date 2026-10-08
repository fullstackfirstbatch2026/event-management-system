package com.eventreg.repository;

import com.eventreg.model.RegistrationResult;
import com.eventreg.model.RegistrationView;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class RegistrationRepository {

    private final JdbcTemplate jdbc;

    // JOIN: registrations + events + participants
    private static final String SELECT_REGISTRATIONS = """
            SELECT r.registration_id, e.event_id, e.title AS event_title, e.event_date, e.venue,
                   p.participant_id, p.name AS participant_name, p.email AS participant_email,
                   r.registration_date
            FROM registrations r
            JOIN events e ON e.event_id = r.event_id
            JOIN participants p ON p.participant_id = r.participant_id
            """;

    private static final RowMapper<RegistrationView> MAPPER = (rs, i) -> new RegistrationView(
            rs.getLong("registration_id"),
            rs.getLong("event_id"),
            rs.getString("event_title"),
            rs.getObject("event_date", LocalDate.class),
            rs.getString("venue"),
            rs.getLong("participant_id"),
            rs.getString("participant_name"),
            rs.getString("participant_email"),
            rs.getObject("registration_date", LocalDateTime.class));

    public RegistrationRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RegistrationView> findAll() {
        return jdbc.query(SELECT_REGISTRATIONS + " ORDER BY r.registration_date DESC, r.registration_id DESC", MAPPER);
    }

    public List<RegistrationView> findByEvent(long eventId) {
        return jdbc.query(SELECT_REGISTRATIONS + " WHERE e.event_id = ? ORDER BY p.name", MAPPER, eventId);
    }

    /** Calls the stored procedure sp_register_participant (see routines.sql). */
    public RegistrationResult register(long eventId, long participantId) {
        return jdbc.execute((ConnectionCallback<RegistrationResult>) con -> {
            try (CallableStatement cs = con.prepareCall("{call sp_register_participant(?, ?, ?, ?)}")) {
                cs.setLong(1, eventId);
                cs.setLong(2, participantId);
                cs.registerOutParameter(3, Types.TINYINT);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();
                return new RegistrationResult(cs.getInt(3) == 1, cs.getString(4));
            }
        });
    }

    /** Cancelling a registration: the AFTER DELETE trigger gives the seat back. */
    public boolean cancel(long registrationId) {
        return jdbc.update("DELETE FROM registrations WHERE registration_id = ?", registrationId) > 0;
    }
}
