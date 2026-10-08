package com.eventreg.controller;

import com.eventreg.model.RegistrationRequest;
import com.eventreg.model.RegistrationResult;
import com.eventreg.model.RegistrationView;
import com.eventreg.repository.RegistrationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController {

    private final RegistrationRepository repo;

    public RegistrationController(RegistrationRepository repo) {
        this.repo = repo;
    }

    // JOIN report: registrations with event and participant details
    @GetMapping
    public List<RegistrationView> list(@RequestParam(required = false) Long eventId) {
        return eventId == null ? repo.findAll() : repo.findByEvent(eventId);
    }

    // Calls the stored procedure sp_register_participant
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(@RequestBody RegistrationRequest request) {
        if (request.eventId() == null || request.participantId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventId and participantId are required");
        }
        RegistrationResult result = repo.register(request.eventId(), request.participantId());
        if (!result.success()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, result.message());
        }
        return Map.of("message", result.message());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable long id) {
        if (!repo.cancel(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registration not found");
        }
    }
}
