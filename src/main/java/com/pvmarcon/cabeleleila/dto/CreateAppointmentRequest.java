package com.pvmarcon.cabeleleila.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

public record CreateAppointmentRequest(
        @NotNull
        @Future
        LocalDateTime dateTime,
        @NotEmpty
        List<@NotNull @Positive Long> salonServiceIds,
        Boolean suggestionAccepted
        ) {}
