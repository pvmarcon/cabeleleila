package com.pvmarcon.cabeleleila.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record WeeklyDashboardResponse(
        LocalDate weekStart,
        LocalDate weekEnd,
        long totalAppointments,
        long completedAppointments,
        long canceledAppointments,
        BigDecimal revenue,
        List<PopularServiceResponse> mostRequestedServices
) {}
