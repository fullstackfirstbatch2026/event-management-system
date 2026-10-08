package com.eventreg.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RegistrationView(Long registrationId, Long eventId, String eventTitle, LocalDate eventDate,
                               String venue, Long participantId, String participantName,
                               String participantEmail, LocalDateTime registeredAt) {
}
