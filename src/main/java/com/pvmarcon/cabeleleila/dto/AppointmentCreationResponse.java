package com.pvmarcon.cabeleleila.dto;

import java.time.LocalDateTime;

public record AppointmentCreationResponse(
        AppointmentCreationOutcome outcome,
        LocalDateTime suggestedDateTime,
        AppointmentResponse appointment
) {
}
