package com.pvmarcon.cabeleleila.dto;

import com.pvmarcon.cabeleleila.model.AppointmentItemStatus;

import java.math.BigDecimal;

public record AppointmentItemResponse(
        Long id,
        Long salonServiceId,
        String salonServiceName,
        BigDecimal priceCharged,
        AppointmentItemStatus status
) {}
