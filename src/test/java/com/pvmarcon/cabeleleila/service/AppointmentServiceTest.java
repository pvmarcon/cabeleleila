package com.pvmarcon.cabeleleila.service;

import com.pvmarcon.cabeleleila.dto.AppointmentCreationOutcome;
import com.pvmarcon.cabeleleila.dto.ChangeAppointmentRequest;
import com.pvmarcon.cabeleleila.dto.CreateAppointmentRequest;
import com.pvmarcon.cabeleleila.exception.AppointmentAccessDeniedException;
import com.pvmarcon.cabeleleila.exception.DuplicateServiceException;
import com.pvmarcon.cabeleleila.exception.InvalidAppointmentDateException;
import com.pvmarcon.cabeleleila.exception.InvalidAppointmentDateRangeException;
import com.pvmarcon.cabeleleila.exception.InvalidAppointmentScheduleException;
import com.pvmarcon.cabeleleila.exception.InvalidSalonServiceException;
import com.pvmarcon.cabeleleila.model.Appointment;
import com.pvmarcon.cabeleleila.model.AppointmentItem;
import com.pvmarcon.cabeleleila.model.AppointmentItemStatus;
import com.pvmarcon.cabeleleila.model.AppointmentStatus;
import com.pvmarcon.cabeleleila.model.Profile;
import com.pvmarcon.cabeleleila.model.SalonService;
import com.pvmarcon.cabeleleila.model.User;
import com.pvmarcon.cabeleleila.repository.AppointmentRepository;
import com.pvmarcon.cabeleleila.repository.SalonServiceRepository;
import com.pvmarcon.cabeleleila.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {
    @Mock AppointmentRepository appointmentRepository;
    @Mock UserRepository userRepository;
    @Mock SalonServiceRepository salonServiceRepository;

    private  AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(appointmentRepository, userRepository, salonServiceRepository);
    }

    @Test
    void rejectsDuplicateServiceIds() {
        Long customerId = 1L;
        User customer = new  User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        var request = new CreateAppointmentRequest(
                futureSlot(2),
                List.of(10L, 10L),
                null
        );

        assertThrows(DuplicateServiceException.class,
                () -> appointmentService.createAppointment(request, customerId));

        verifyNoInteractions(appointmentRepository, salonServiceRepository);
    }

    @Test
    void rejectsAppointmentWhenServiceDoesNotExist() {
        Long customerId = 1L;
        Long invalidServiceId = 999L;
        User customer = new  User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );

        var request = new CreateAppointmentRequest(
                futureSlot(2),
                List.of(invalidServiceId),
                null
        );

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        when(salonServiceRepository.findAllById(Set.of(invalidServiceId))).thenReturn(List.of());

        assertThrows(InvalidSalonServiceException.class,
                () -> appointmentService.createAppointment(request, customerId));

        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void rejectsAppointmentWhenServiceIsInactive() {
        Long customerId = 1L;
        User customer = new  User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );

        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        var inactiveService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00"));

        inactiveService.setActive(false);

        when(salonServiceRepository.findAllById(Set.of(7L)))
                .thenReturn(List.of(inactiveService));

        var request = new CreateAppointmentRequest(
                futureSlot(2),
                List.of(7L),
                null
        );

        assertThrows(InvalidSalonServiceException.class,
                () -> appointmentService.createAppointment(request, customerId));

        verifyNoInteractions(appointmentRepository);
    }

    @Test
    void createsAppointmentWhenNoActiveAppointmentExistsInWeek() {
        Long customerId = 1L;
        User customer = new  User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        var activeService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        when(salonServiceRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(activeService));

        when(appointmentRepository
                .findFirstByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanAndStatusInOrderByDateTimeAsc(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        anyList()
                ))
                .thenReturn(Optional.empty());

        var request = new CreateAppointmentRequest(
                futureSlot(2),
                List.of(1L),
                null
        );

        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = appointmentService.createAppointment(request, customerId);

        assertEquals(AppointmentCreationOutcome.CREATED, response.outcome());
        assertEquals(new BigDecimal("35.00"),
                response.appointment().items().get(0).priceCharged());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void returnsSuggestionWhenCustomerHasActiveAppointmentInWeek() {
        Long customerId = 1L;
        Long serviceId = 7L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        SalonService activeService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        when(salonServiceRepository.findAllById(Set.of(serviceId)))
                .thenReturn(List.of(activeService));

        LocalDateTime requestedDateTime = futureSlot(3);
        LocalDateTime suggestedDateTime = futureSlot(2);
        Appointment existingAppointment = new Appointment(customer, suggestedDateTime);
        when(appointmentRepository
                .findFirstByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanAndStatusInOrderByDateTimeAsc(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        anyList()
                ))
                .thenReturn(Optional.of(existingAppointment));

        var request = new CreateAppointmentRequest(
                requestedDateTime,
                List.of(serviceId),
                null
        );

        var response = appointmentService.createAppointment(request, customerId);

        assertEquals(AppointmentCreationOutcome.SUGGESTION_REQUIRED, response.outcome());
        assertEquals(suggestedDateTime, response.suggestedDateTime());
        assertNull(response.appointment());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void createsAppointmentOnSuggestedDateWhenSuggestionIsAccepted() {
        Long customerId = 1L;
        Long serviceId = 7L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        SalonService activeService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        when(salonServiceRepository.findAllById(Set.of(serviceId)))
                .thenReturn(List.of(activeService));

        LocalDateTime requestedDateTime = futureSlot(3);
        LocalDateTime suggestedDateTime = futureSlot(2);
        Appointment existingAppointment = new Appointment(customer, suggestedDateTime);
        when(appointmentRepository
                .findFirstByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanAndStatusInOrderByDateTimeAsc(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        anyList()
                ))
                .thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateAppointmentRequest(
                requestedDateTime,
                List.of(serviceId),
                true
        );

        var response = appointmentService.createAppointment(request, customerId);

        assertEquals(AppointmentCreationOutcome.CREATED, response.outcome());
        assertEquals(suggestedDateTime, response.appointment().dateTime());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void createsAppointmentOnRequestedDateWhenSuggestionIsDeclined() {
        Long customerId = 1L;
        Long serviceId = 7L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        SalonService activeService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        when(salonServiceRepository.findAllById(Set.of(serviceId)))
                .thenReturn(List.of(activeService));

        LocalDateTime requestedDateTime = futureSlot(3);
        LocalDateTime suggestedDateTime = futureSlot(2);
        Appointment existingAppointment = new Appointment(customer, suggestedDateTime);
        when(appointmentRepository
                .findFirstByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanAndStatusInOrderByDateTimeAsc(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        anyList()
                ))
                .thenReturn(Optional.of(existingAppointment));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CreateAppointmentRequest(
                requestedDateTime,
                List.of(serviceId),
                false
        );

        var response = appointmentService.createAppointment(request, customerId);

        assertEquals(AppointmentCreationOutcome.CREATED, response.outcome());
        assertEquals(requestedDateTime, response.appointment().dateTime());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void rejectsAppointmentOnSunday() {
        LocalDate nextSunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        var request = new CreateAppointmentRequest(
                nextSunday.atTime(10, 0),
                List.of(1L),
                null
        );

        assertThrows(InvalidAppointmentScheduleException.class,
                () -> appointmentService.createAppointment(request, 1L));

        verifyNoInteractions(userRepository, salonServiceRepository, appointmentRepository);
    }

    @Test
    void rejectsAppointmentOutsideBusinessHours() {
        var request = new CreateAppointmentRequest(
                futureSlot(0).withHour(8),
                List.of(1L),
                null
        );

        assertThrows(InvalidAppointmentScheduleException.class,
                () -> appointmentService.createAppointment(request, 1L));

        verifyNoInteractions(userRepository, salonServiceRepository, appointmentRepository);
    }

    @Test
    void rejectsAppointmentThatDoesNotStartOnAWholeHour() {
        var request = new CreateAppointmentRequest(
                futureSlot(0).withMinute(30),
                List.of(1L),
                null
        );

        assertThrows(InvalidAppointmentScheduleException.class,
                () -> appointmentService.createAppointment(request, 1L));

        verifyNoInteractions(userRepository, salonServiceRepository, appointmentRepository);
    }

    @Test
    void acceptsOpeningAndClosingHours() {
        Long customerId = 1L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        SalonService activeService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        when(salonServiceRepository.findAllById(Set.of(1L)))
                .thenReturn(List.of(activeService));
        when(appointmentRepository
                .findFirstByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanAndStatusInOrderByDateTimeAsc(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        anyList()
                ))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDateTime openingTime = nextMonday.atTime(9, 0);
        LocalDateTime closingTime = nextMonday.atTime(16, 0);

        var openingAppointment = appointmentService.createAppointment(
                new CreateAppointmentRequest(openingTime, List.of(1L), null),
                customerId
        );
        var closingAppointment = appointmentService.createAppointment(
                new CreateAppointmentRequest(closingTime, List.of(1L), null),
                customerId
        );

        assertEquals(openingTime, openingAppointment.appointment().dateTime());
        assertEquals(closingTime, closingAppointment.appointment().dateTime());
        verify(appointmentRepository, times(2)).save(any(Appointment.class));
    }

    @Test
    void rejectsSuggestedAppointmentOutsideScheduleWhenSuggestionIsAccepted() {
        Long customerId = 1L;
        Long serviceId = 7L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        SalonService activeService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        when(salonServiceRepository.findAllById(Set.of(serviceId)))
                .thenReturn(List.of(activeService));

        LocalDateTime invalidSuggestedDateTime = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                .atTime(10, 0);
        Appointment existingAppointment = new Appointment(customer, invalidSuggestedDateTime);
        when(appointmentRepository
                .findFirstByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanAndStatusInOrderByDateTimeAsc(
                        anyLong(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class),
                        anyList()
                ))
                .thenReturn(Optional.of(existingAppointment));

        var request = new CreateAppointmentRequest(
                futureSlot(3),
                List.of(serviceId),
                true
        );

        assertThrows(InvalidAppointmentScheduleException.class,
                () -> appointmentService.createAppointment(request, customerId));

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void changesAppointmentWhenCustomerOwnsItAndItIsMoreThanTwoDaysAway() {
        Long customerId = 0L;
        Long appointmentId = 12L;
        Long serviceId = 7L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        Appointment appointment = new Appointment(customer, futureSlot(2));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        SalonService activeService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        when(salonServiceRepository.findAllById(Set.of(serviceId)))
                .thenReturn(List.of(activeService));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newDateTime = futureSlot(5);
        var request = new ChangeAppointmentRequest(newDateTime, List.of(serviceId));

        var response = appointmentService.changeAppointment(request, customerId, appointmentId);

        assertEquals(newDateTime, response.dateTime());
        assertEquals(new BigDecimal("35.00"), response.items().get(0).priceCharged());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void rejectsChangingAppointmentLessThanTwoDaysAway() {
        Long customerId = 0L;
        Long appointmentId = 12L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        Appointment appointment = new Appointment(customer, LocalDate.now().plusDays(1).atTime(10, 0));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        var request = new ChangeAppointmentRequest(
                futureSlot(5),
                List.of(7L)
        );

        assertThrows(InvalidAppointmentDateException.class,
                () -> appointmentService.changeAppointment(request, customerId, appointmentId));

        verify(appointmentRepository, never()).save(any(Appointment.class));
        verifyNoInteractions(salonServiceRepository);
    }

    @Test
    void rejectsChangingAnotherCustomersAppointment() {
        Long appointmentId = 12L;
        User owner = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        Appointment appointment = new Appointment(owner, futureSlot(2));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        var request = new ChangeAppointmentRequest(
                futureSlot(5),
                List.of(7L)
        );

        assertThrows(AppointmentAccessDeniedException.class,
                () -> appointmentService.changeAppointment(request, 1L, appointmentId));

        verify(appointmentRepository, never()).save(any(Appointment.class));
        verifyNoInteractions(salonServiceRepository);
    }

    @Test
    void rejectsChangingAppointmentToSunday() {
        Long customerId = 0L;
        Long appointmentId = 12L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        Appointment appointment = new Appointment(customer, futureSlot(2));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        LocalDate nextSunday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        var request = new ChangeAppointmentRequest(nextSunday.atTime(10, 0), List.of(7L));

        assertThrows(InvalidAppointmentScheduleException.class,
                () -> appointmentService.changeAppointment(request, customerId, appointmentId));

        verify(appointmentRepository, never()).save(any(Appointment.class));
        verifyNoInteractions(salonServiceRepository);
    }

    @Test
    void confirmsAppointment() {
        Long appointmentId = 12L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        Appointment appointment = new Appointment(customer, futureSlot(2));
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        var response = appointmentService.confirmAppointment(appointmentId);

        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, response.status());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void updatesAppointmentServiceStatus() {
        Long appointmentId = 12L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        SalonService salonService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        Appointment appointment = new Appointment(customer, futureSlot(2));
        AppointmentItem appointmentItem =
                new AppointmentItem(salonService, new BigDecimal("35.00"));
        appointment.addItem(appointmentItem);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        var response = appointmentService.updateServiceStatus(
                appointmentId,
                appointmentItem.getId(),
                AppointmentItemStatus.IN_PROGRESS
        );

        assertEquals(AppointmentItemStatus.IN_PROGRESS, appointmentItem.getStatus());
        assertEquals(AppointmentItemStatus.IN_PROGRESS, response.items().get(0).status());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void marksAppointmentCompletedWhenAllServicesAreCompleted() {
        Long appointmentId = 12L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        SalonService salonService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        Appointment appointment = new Appointment(customer, futureSlot(2));
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        AppointmentItem appointmentItem =
                new AppointmentItem(salonService, new BigDecimal("35.00"));
        appointment.addItem(appointmentItem);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        var response = appointmentService.updateServiceStatus(
                appointmentId,
                appointmentItem.getId(),
                AppointmentItemStatus.COMPLETED
        );

        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());
        assertEquals(AppointmentStatus.COMPLETED, response.status());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void allowsAdminToChangeAppointmentInsideCustomerChangeWindow() {
        Long appointmentId = 12L;
        Long serviceId = 7L;
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        Appointment appointment = new Appointment(
                customer,
                LocalDate.now().plusDays(1).atTime(10, 0)
        );
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));

        SalonService activeService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        when(salonServiceRepository.findAllById(Set.of(serviceId)))
                .thenReturn(List.of(activeService));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime newDateTime = futureSlot(5);
        var response = appointmentService.changeAppointmentAsAdmin(
                new ChangeAppointmentRequest(newDateTime, List.of(serviceId)),
                appointmentId
        );

        assertEquals(newDateTime, response.dateTime());
        assertEquals(AppointmentStatus.CONFIRMED, response.status());
        verify(appointmentRepository).save(appointment);
    }

    @Test
    void calculatesWeeklyDashboardMetrics() {
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        SalonService salonService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        LocalDateTime monday = futureSlot(0);

        Appointment pending = new Appointment(customer, monday);
        pending.addItem(new AppointmentItem(salonService, new BigDecimal("35.00")));
        pending.addItem(new AppointmentItem(salonService, new BigDecimal("35.00")));

        Appointment completed = new Appointment(customer, monday.plusDays(1));
        completed.setStatus(AppointmentStatus.COMPLETED);
        completed.addItem(new AppointmentItem(salonService, new BigDecimal("35.00")));

        Appointment canceled = new Appointment(customer, monday.plusDays(2));
        canceled.setStatus(AppointmentStatus.CANCELED);
        canceled.addItem(new AppointmentItem(salonService, new BigDecimal("35.00")));

        when(appointmentRepository
                .findAllByDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
                        monday.toLocalDate().atStartOfDay(),
                        monday.toLocalDate().plusWeeks(1).atStartOfDay()
                ))
                .thenReturn(List.of(pending, completed, canceled));

        var dashboard = appointmentService.getWeeklyDashboard(monday.toLocalDate());

        assertEquals(3, dashboard.totalAppointments());
        assertEquals(1, dashboard.completedAppointments());
        assertEquals(1, dashboard.canceledAppointments());
        assertEquals(new BigDecimal("35.00"), dashboard.revenue());
        assertEquals(1, dashboard.mostRequestedServices().size());
        assertEquals(3, dashboard.mostRequestedServices().get(0).requestCount());
    }

    @Test
    void returnsCustomerAppointmentHistoryWithinRequestedPeriod() {
        Long customerId = 1L;
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        Appointment appointment = new Appointment(
                customer, LocalDateTime.of(2026, 10, 24, 14, 0)
        );
        SalonService salonService = new SalonService(
                "Haircut", "Test service", 30, new BigDecimal("35.00")
        );
        appointment.addItem(new AppointmentItem(salonService, new BigDecimal("35.00")));

        when(appointmentRepository
                .findByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
                        customerId,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay()
                ))
                .thenReturn(List.of(appointment));

        List<com.pvmarcon.cabeleleila.dto.AppointmentResponse> history =
                appointmentService.getAppointmentHistory(customerId, from, to);

        assertEquals(1, history.size());
        assertEquals(appointment.getDateTime(), history.get(0).dateTime());
        assertEquals(new BigDecimal("35.00"), history.get(0).items().get(0).priceCharged());
    }

    @Test
    void returnsEmptyHistoryWhenCustomerHasNoAppointmentsInPeriod() {
        Long customerId = 1L;
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        User customer = new User(
                "Test Client", "test@example.test", "555-0100",
                "test-hash", Profile.CLIENT
        );
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(appointmentRepository
                .findByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
                        customerId,
                        from.atStartOfDay(),
                        to.plusDays(1).atStartOfDay()
                ))
                .thenReturn(List.of());

        var history = appointmentService.getAppointmentHistory(customerId, from, to);

        assertEquals(List.of(), history);
    }

    @Test
    void rejectsHistoryWhenEndDateIsBeforeStartDate() {
        LocalDate from = LocalDate.of(2026, 10, 31);
        LocalDate to = LocalDate.of(2026, 10, 1);

        assertThrows(InvalidAppointmentDateRangeException.class,
                () -> appointmentService.getAppointmentHistory(1L, from, to));

        verifyNoInteractions(userRepository, appointmentRepository);
    }

    private LocalDateTime futureSlot(int daysAfterNextMonday) {
        LocalDate nextMonday = LocalDate.now()
                .with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return nextMonday.plusDays(daysAfterNextMonday).atTime(10, 0);
    }
}
