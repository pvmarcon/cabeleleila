package com.pvmarcon.cabeleleila.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCustomerNotFound
            (CustomerNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(DuplicateServiceException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateService
            (DuplicateServiceException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidSalonServiceException.class)
    public ResponseEntity<Map<String, String>> handleInvalidSalonService
            (InvalidSalonServiceException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleAppointmentNotFound
            (AppointmentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AppointmentAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAppointmentAccessDenied
            (AppointmentAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidAppointmentDateException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAppointmentDate
            (InvalidAppointmentDateException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidAppointmentDateRangeException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAppointmentDateRange
            (InvalidAppointmentDateRangeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidAppointmentScheduleException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAppointmentSchedule(
            InvalidAppointmentScheduleException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(InvalidAppointmentStatusException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAppointmentStatus(
            InvalidAppointmentStatusException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", exception.getMessage()));
    }
}