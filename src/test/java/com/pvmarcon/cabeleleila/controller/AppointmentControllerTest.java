package com.pvmarcon.cabeleleila.controller;

import com.pvmarcon.cabeleleila.dto.AppointmentCreationOutcome;
import com.pvmarcon.cabeleleila.dto.AppointmentCreationResponse;
import com.pvmarcon.cabeleleila.dto.AppointmentResponse;
import com.pvmarcon.cabeleleila.dto.CreateAppointmentRequest;
import com.pvmarcon.cabeleleila.exception.CustomerNotFoundException;
import com.pvmarcon.cabeleleila.exception.GlobalExceptionHandler;
import com.pvmarcon.cabeleleila.exception.InvalidAppointmentDateRangeException;
import com.pvmarcon.cabeleleila.model.AppointmentStatus;
import com.pvmarcon.cabeleleila.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AppointmentController(appointmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsBadRequestWhenCreateRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/appointments")
                        .header("X-Customer-Id", 1)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dateTime": null,
                                  "salonServiceIds": [],
                                  "suggestionAccepted": null
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointmentService);
    }

    @Test
    void returnsBadRequestWhenChangeRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/appointments/12")
                        .header("X-Customer-Id", 1)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dateTime": null,
                                  "salonServiceIds": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointmentService);
    }

    @Test
    void returnsCreatedWhenServiceCreatesAppointment() throws Exception {
        Long customerId = 1L;
        LocalDateTime dateTime = LocalDateTime.now().plusDays(5);
        AppointmentResponse appointment = new AppointmentResponse(
                12L, dateTime, AppointmentStatus.PENDING, List.of()
        );
        when(appointmentService.createAppointment(any(CreateAppointmentRequest.class), eq(customerId)))
                .thenReturn(new AppointmentCreationResponse(
                        AppointmentCreationOutcome.CREATED, null, appointment
                ));

        mockMvc.perform(post("/appointments")
                        .header("X-Customer-Id", customerId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dateTime": "%s",
                                  "salonServiceIds": [7],
                                  "suggestionAccepted": null
                                }
                                """.formatted(dateTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outcome").value("CREATED"))
                .andExpect(jsonPath("$.appointment.id").value(12));
    }

    @Test
    void returnsOkWhenServiceRequiresSuggestion() throws Exception {
        Long customerId = 1L;
        LocalDateTime suggestedDateTime = LocalDateTime.now().plusDays(4);
        when(appointmentService.createAppointment(any(CreateAppointmentRequest.class), eq(customerId)))
                .thenReturn(new AppointmentCreationResponse(
                        AppointmentCreationOutcome.SUGGESTION_REQUIRED,
                        suggestedDateTime,
                        null
                ));

        mockMvc.perform(post("/appointments")
                        .header("X-Customer-Id", customerId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dateTime": "%s",
                                  "salonServiceIds": [7],
                                  "suggestionAccepted": null
                                }
                                """.formatted(LocalDateTime.now().plusDays(5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome").value("SUGGESTION_REQUIRED"))
                .andExpect(jsonPath("$.suggestedDateTime").exists());
    }

    @Test
    void mapsMissingCustomerToNotFound() throws Exception {
        Long customerId = 404L;
        when(appointmentService.createAppointment(any(CreateAppointmentRequest.class), eq(customerId)))
                .thenThrow(new CustomerNotFoundException("Customer not found"));

        mockMvc.perform(post("/appointments")
                        .header("X-Customer-Id", customerId)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "dateTime": "%s",
                                  "salonServiceIds": [7],
                                  "suggestionAccepted": null
                                }
                                """.formatted(LocalDateTime.now().plusDays(5))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Customer not found"));
    }

    @Test
    void mapsInvertedHistoryPeriodToBadRequest() throws Exception {
        Long customerId = 1L;
        LocalDate from = LocalDate.of(2026, 10, 31);
        LocalDate to = LocalDate.of(2026, 10, 1);
        when(appointmentService.getAppointmentHistory(customerId, from, to))
                .thenThrow(new InvalidAppointmentDateRangeException("Invalid period"));

        mockMvc.perform(get("/appointments")
                        .header("X-Customer-Id", customerId)
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid period"));
    }
}
