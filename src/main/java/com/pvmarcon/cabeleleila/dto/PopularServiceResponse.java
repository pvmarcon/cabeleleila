package com.pvmarcon.cabeleleila.dto;

public record PopularServiceResponse(
        Long salonServiceId,
        String salonServiceName,
        long requestCount
) {}
