package com.eventreg.repository;

import com.eventreg.model.Organizer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class OrganizerRepository {

    private final JdbcTemplate jdbc;

    private static final RowMapper<Organizer> MAPPER = (rs, i) -> new Organizer(
            rs.getLong("organizer_id"), rs.getString("name"), rs.getString("email"), rs.getString("phone"));

    public OrganizerRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Organizer> findAll() {
        return jdbc.query("SELECT organizer_id, name, email, phone FROM organizers ORDER BY name", MAPPER);
    }

    public Organizer create(Organizer o) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO organizers (name, email, phone) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, o.name());
            ps.setString(2, o.email());
            ps.setString(3, o.phone());
            return ps;
        }, keys);
        return new Organizer(keys.getKey().longValue(), o.name(), o.email(), o.phone());
    }

    public boolean delete(long id) {
        return jdbc.update("DELETE FROM organizers WHERE organizer_id = ?", id) > 0;
    }
}
