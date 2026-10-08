package com.eventreg.controller;

import com.eventreg.model.Participant;
import com.eventreg.repository.ParticipantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/participants")
public class ParticipantController {

    private final ParticipantRepository repo;

    public ParticipantController(ParticipantRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Participant> list() {
        return repo.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Participant create(@RequestBody Participant participant) {
        if (isBlank(participant.name()) || isBlank(participant.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and email are required");
        }
        return repo.create(participant);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        if (!repo.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participant not found");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
