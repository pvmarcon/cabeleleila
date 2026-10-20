package com.pvmarcon.cabeleleila.controller;

import com.pvmarcon.cabeleleila.dto.*;
import com.pvmarcon.cabeleleila.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> read(
            @RequestHeader("X-Customer-Id") Long customerId,
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to
    ) {
        List<AppointmentResponse> response = appointmentService.getAppointmentHistory(customerId, from, to);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping
    public ResponseEntity<AppointmentCreationResponse> create(
            @RequestBody @Valid CreateAppointmentRequest dto,
            @RequestHeader("X-Customer-Id") Long customerId
            ) {
        AppointmentCreationResponse response = appointmentService.createAppointment(dto, customerId);

        if (response.outcome() == AppointmentCreationOutcome.SUGGESTION_REQUIRED) {
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<AppointmentResponse> update(
            @PathVariable Long appointmentId,
            @RequestBody @Valid ChangeAppointmentRequest dto,
            @RequestHeader("X-Customer-Id") Long customerId
    ) {
        AppointmentResponse response = appointmentService.changeAppointment(dto, customerId, appointmentId);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
