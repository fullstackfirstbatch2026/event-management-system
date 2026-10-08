package com.eventreg.repository;

import com.eventreg.model.Participant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class ParticipantRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Participant> MAPPER = (rs, i) -> new Participant(
            rs.getLong("participant_id"), rs.getString("name"), rs.getString("email"), rs.getString("phone"));

    public ParticipantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Participant> findAll() {
        return jdbc.query("SELECT participant_id, name, email, phone FROM participants ORDER BY name", MAPPER);
    }

    public Participant create(Participant p) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO participants (name, email, phone) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.name());
            ps.setString(2, p.email());
            ps.setString(3, p.phone());
            return ps;
        }, keys);
        return new Participant(keys.getKey().longValue(), p.name(), p.email(), p.phone());
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM participants WHERE participant_id = ?", id) > 0;
    }
}
