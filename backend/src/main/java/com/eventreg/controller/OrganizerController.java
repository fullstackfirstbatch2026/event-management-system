package com.eventreg.controller;

import com.eventreg.model.Organizer;
import com.eventreg.repository.OrganizerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/organizers")
public class OrganizerController {

    private final OrganizerRepository repo;

    public OrganizerController(OrganizerRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Organizer> list() {
        return repo.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Organizer create(@RequestBody Organizer organizer) {
        if (isBlank(organizer.name()) || isBlank(organizer.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and email are required");
        }
        return repo.create(organizer);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        if (!repo.delete(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Organizer not found");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
