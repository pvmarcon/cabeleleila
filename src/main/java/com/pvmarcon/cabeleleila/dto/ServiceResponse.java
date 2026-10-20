package com.pvmarcon.cabeleleila.dto;

import java.math.BigDecimal;

public record ServiceResponse(
        Long id,
        String name,
        String description,
        int durationMinutes,
        BigDecimal price
) {
}
