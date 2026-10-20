package com.pvmarcon.cabeleleila.controller;

import com.pvmarcon.cabeleleila.dto.AdminAppointmentResponse;
import com.pvmarcon.cabeleleila.dto.ChangeAppointmentRequest;
import com.pvmarcon.cabeleleila.dto.WeeklyDashboardResponse;
import com.pvmarcon.cabeleleila.model.AppointmentItemStatus;
import com.pvmarcon.cabeleleila.service.AppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/appointments")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    public AdminAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    public ResponseEntity<List<AdminAppointmentResponse>> getAllAppointments() {
        List<AdminAppointmentResponse> response = appointmentService.getAllAppointments();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/dashboard")
    public ResponseEntity<WeeklyDashboardResponse> getWeeklyDashboard() {
        return ResponseEntity.ok(appointmentService.getWeeklyDashboard(LocalDate.now()));
    }

    @PutMapping("/{appointmentId}")
    public ResponseEntity<AdminAppointmentResponse> updateAppointment(
            @PathVariable Long appointmentId,
            @RequestBody @Valid ChangeAppointmentRequest request
    ) {
        return ResponseEntity.ok(appointmentService.changeAppointmentAsAdmin(request, appointmentId));
    }

    @PutMapping("/{appointmentId}/confirm")
    public ResponseEntity<AdminAppointmentResponse> confirmAppointment(
            @PathVariable Long appointmentId) {
        AdminAppointmentResponse response = appointmentService.confirmAppointment(appointmentId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResponseEntity<AdminAppointmentResponse> cancelAppointment(
            @PathVariable Long appointmentId
    ) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(appointmentId));
    }

    @PutMapping("/{appointmentId}/services/{itemId}/status")
    public ResponseEntity<AdminAppointmentResponse> updateServiceStatus(
            @PathVariable Long appointmentId,
            @PathVariable Long itemId,
            @RequestParam AppointmentItemStatus status
    ) {
        AdminAppointmentResponse response =
                appointmentService.updateServiceStatus(appointmentId, itemId, status);
        return ResponseEntity.ok(response);
    }
}
