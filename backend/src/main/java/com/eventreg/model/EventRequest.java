package com.eventreg.model;

import java.time.LocalDate;

public record EventRequest(String title, String description, LocalDate eventDate, String venue,
                           Integer totalSeats, Long organizerId) {
}
