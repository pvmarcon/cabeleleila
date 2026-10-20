package com.pvmarcon.cabeleleila.dto;

import com.pvmarcon.cabeleleila.model.AppointmentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record AdminAppointmentResponse(
        Long id,
        LocalDateTime dateTime,
        AppointmentStatus status,
        List<AppointmentItemResponse> items,
        String customerName
) {}
