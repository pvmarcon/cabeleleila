package com.pvmarcon.cabeleleila.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;
import java.util.List;

public record ChangeAppointmentRequest(
        @NotNull
        @Future
        LocalDateTime dateTime,
        @NotEmpty
        List<@NotNull @Positive Long> salonServiceIds
) {}
