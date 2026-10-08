package com.eventreg.model;

import java.time.LocalDate;

public record EventView(Long id, String title, String description, LocalDate eventDate, String venue,
                        int totalSeats, int availableSeats, Long organizerId, String organizerName,
                        int registrationCount) {
}
