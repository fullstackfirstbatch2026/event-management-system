package com.eventreg.controller;

import com.eventreg.model.EventRequest;
import com.eventreg.model.EventView;
import com.eventreg.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventRepository repo;

    public EventController(EventRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<EventView> list() {
        return repo.findAll();
    }

    // Subquery report
    @GetMapping("/above-average-registrations")
    public List<EventView> aboveAverage() {
        return repo.findAboveAverageRegistrations();
    }

    @GetMapping("/{id}")
    public EventView get(@PathVariable long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    // Calls the stored function fn_event_registration_count
    @GetMapping("/{id}/registration-count")
    public Map<String, Object> registrationCount(@PathVariable long id) {
        EventView event = get(id);
        return Map.of("eventId", id, "title", event.title(), "registrationCount", repo.registrationCount(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventView create(@RequestBody EventRequest request) {
        if (request.title() == null || request.title().isBlank() || request.venue() == null || request.venue().isBlank()
                || request.eventDate() == null || request.organizerId() == null
                || request.totalSeats() == null || request.totalSeats() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "title, venue, eventDate, organizerId and totalSeats (>= 1) are required");
        }
        if (request.eventDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event date cannot be in the past");
        }
        return get(repo.create(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        if (!repo.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
    }
}
