package com.eventreg.config;

import com.eventreg.model.EventRequest;
import com.eventreg.model.Organizer;
import com.eventreg.model.Participant;
import com.eventreg.repository.EventRepository;
import com.eventreg.repository.OrganizerRepository;
import com.eventreg.repository.ParticipantRepository;
import com.eventreg.repository.RegistrationRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * On startup: (1) creates the stored function, procedure and triggers from routines.sql,
 * (2) adds a little sample data if the database is empty.
 */
@Component
public class DatabaseInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final OrganizerRepository organizers;
    private final ParticipantRepository participants;
    private final EventRepository events;
    private final RegistrationRepository registrations;

    public DatabaseInitializer(JdbcTemplate jdbc, OrganizerRepository organizers, ParticipantRepository participants,
                               EventRepository events, RegistrationRepository registrations) {
        this.jdbc = jdbc;
        this.organizers = organizers;
        this.participants = participants;
        this.events = events;
        this.registrations = registrations;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        createRoutines();
        seedSampleData();
    }

    /** routines.sql statements are separated by a line containing only "//" (the DELIMITER used for MySQL clients). */
    private void createRoutines() throws Exception {
        StringBuilder statement = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource("routines.sql").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.toUpperCase().startsWith("DELIMITER")
                        || trimmed.toUpperCase().startsWith("USE ")) {
                    continue;
                }
                if (trimmed.equals("//")) {
                    runIfNotBlank(statement.toString());
                    statement.setLength(0);
                } else {
                    statement.append(line).append('\n');
                }
            }
            runIfNotBlank(statement.toString());
        }
    }

    private void runIfNotBlank(String sql) {
        if (!sql.isBlank()) {
            jdbc.execute(sql);
        }
    }

    private void seedSampleData() {
        if (!organizers.findAll().isEmpty()) {
            return;
        }
        Organizer techClub = organizers.create(new Organizer(null, "Tech Club", "techclub@example.com", "9840000001"));
        Organizer artsSociety = organizers.create(new Organizer(null, "Arts Society", "arts@example.com", "9840000002"));

        Participant arun = participants.create(new Participant(null, "Arun Kumar", "arun@example.com", "9841111111"));
        Participant priya = participants.create(new Participant(null, "Priya Nair", "priya@example.com", "9842222222"));
        Participant karthik = participants.create(new Participant(null, "Karthik Raj", "karthik@example.com", "9843333333"));
        Participant meena = participants.create(new Participant(null, "Meena S", "meena@example.com", "9844444444"));

        LocalDate today = LocalDate.now();
        long java = events.create(new EventRequest("Java Workshop", "Hands-on Spring Boot session",
                today.plusDays(10), "Lab 2", 30, techClub.id()));
        long hack = events.create(new EventRequest("Hackathon", "24-hour coding event",
                today.plusDays(20), "Main Hall", 3, techClub.id()));
        long art = events.create(new EventRequest("Art Exhibition", "Student paintings and sketches",
                today.plusDays(15), "Gallery", 50, artsSociety.id()));
        events.create(new EventRequest("Photography Walk", "Morning walk with cameras",
                today.plusDays(25), "City Park", 20, artsSociety.id()));

        registrations.register(java, arun.id());
        registrations.register(java, priya.id());
        registrations.register(java, karthik.id());
        registrations.register(hack, arun.id());
        registrations.register(hack, meena.id());
        registrations.register(art, priya.id());
    }
}
