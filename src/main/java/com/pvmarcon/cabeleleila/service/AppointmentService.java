package com.pvmarcon.cabeleleila.service;

import com.pvmarcon.cabeleleila.dto.*;
import com.pvmarcon.cabeleleila.exception.*;
import com.pvmarcon.cabeleleila.model.*;
import com.pvmarcon.cabeleleila.repository.AppointmentRepository;
import com.pvmarcon.cabeleleila.repository.SalonServiceRepository;
import com.pvmarcon.cabeleleila.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final SalonServiceRepository salonServiceRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            UserRepository userRepository,
            SalonServiceRepository salonServiceRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
        this.salonServiceRepository = salonServiceRepository;
    }

    @Transactional
    public AppointmentCreationResponse createAppointment(CreateAppointmentRequest dto, Long customerId) {
        validateAppointmentSchedule(dto.dateTime());
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        List<SalonService> salonServices =
                loadAndValidateActiveServices(dto.salonServiceIds());

        Optional<Appointment> firstAppointmentInWeek =
                findFirstActiveAppointmentInWeek(dto.dateTime(), customer.getId());

        if(firstAppointmentInWeek.isPresent() && dto.suggestionAccepted() == null) {
            return new AppointmentCreationResponse(
                    AppointmentCreationOutcome.SUGGESTION_REQUIRED,
                    firstAppointmentInWeek.get().getDateTime(),
                    null
            );
        }

        LocalDateTime scheduledAt = dto.dateTime();
        if (Boolean.TRUE.equals(dto.suggestionAccepted())) {
            scheduledAt = firstAppointmentInWeek
                    .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found.")).getDateTime();
        }

        validateAppointmentSchedule(scheduledAt);

        Appointment appointment = new Appointment(customer, scheduledAt);

        for (SalonService salonService : salonServices) {
            AppointmentItem appointmentItem = new AppointmentItem(
                    salonService,
                    salonService.getPrice()
            );
            appointment.addItem(appointmentItem);

        }

        Appointment savedAppointment = appointmentRepository.save(appointment);

        AppointmentResponse appointmentResponse = toAppointmentResponse(savedAppointment);

        return new AppointmentCreationResponse(
            AppointmentCreationOutcome.CREATED,
                null,
                appointmentResponse
        );
    }

    @Transactional
    public AppointmentResponse changeAppointment(ChangeAppointmentRequest dto, Long customerId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException("Appointment not found with ID: " + appointmentId));

        if (customerId != appointment.getCustomer().getId()) {
            throw new AppointmentAccessDeniedException("Appointment not found");
        }

        ensureEditable(appointment);

        if (appointment.getDateTime().isBefore(LocalDateTime.now().plusDays(2))) {
            throw new InvalidAppointmentDateException("Appointment can no longer be changed online.");
        }

        validateAppointmentSchedule(dto.dateTime());

        List<SalonService> salonServices =
                loadAndValidateActiveServices(dto.salonServiceIds());

        appointment.setDateTime(dto.dateTime());
        appointment.getItems().clear(); // orphanRemoval=true remove os itens anteriores

        for (SalonService salonService : salonServices) {
            appointment.addItem(new AppointmentItem(salonService, salonService.getPrice()));
        }

        Appointment savedAppointment = appointmentRepository.save(appointment);

        return toAppointmentResponse(savedAppointment);
    }

    @Transactional
    public AdminAppointmentResponse changeAppointmentAsAdmin(
            ChangeAppointmentRequest dto,
            Long appointmentId
    ) {
        Appointment appointment = findAppointment(appointmentId);
        ensureEditable(appointment);
        validateAppointmentSchedule(dto.dateTime());

        List<SalonService> salonServices =
                loadAndValidateActiveServices(dto.salonServiceIds());

        appointment.setDateTime(dto.dateTime());
        appointment.getItems().clear();
        for (SalonService salonService : salonServices) {
            appointment.addItem(new AppointmentItem(salonService, salonService.getPrice()));
        }

        return toAdminAppointmentResponse(appointmentRepository.save(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentHistory(Long customerId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new InvalidAppointmentDateRangeException("Appointment to be before from.");
        }

        if(userRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found with ID: " + customerId);
        }

        LocalDateTime startInclusive = from.atStartOfDay();
        LocalDateTime endExclusive = to.plusDays(1).atStartOfDay();

        List<Appointment> appointments = appointmentRepository.findByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
                customerId,
                startInclusive,
                endExclusive
        );

        return appointments.stream()
                .map(this::toAppointmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminAppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAllByOrderByDateTimeAsc()
                .stream()
                .map(this::toAdminAppointmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WeeklyDashboardResponse getWeeklyDashboard(LocalDate dateInWeek) {
        LocalDate weekStart = dateInWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);
        List<Appointment> appointments =
                appointmentRepository.findAllByDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
                        weekStart.atStartOfDay(),
                        weekStart.plusWeeks(1).atStartOfDay()
                );

        long completedCount = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .count();
        long canceledCount = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.CANCELED)
                .count();

        BigDecimal revenue = appointments.stream()
                .filter(appointment -> appointment.getStatus() == AppointmentStatus.COMPLETED)
                .flatMap(appointment -> appointment.getItems().stream())
                .map(AppointmentItem::getPriceCharged)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<Long, Long> serviceCounts = new HashMap<>();
        Map<Long, String> serviceNames = new HashMap<>();
        appointments.stream()
                .filter(appointment -> appointment.getStatus() != AppointmentStatus.CANCELED)
                .flatMap(appointment -> appointment.getItems().stream())
                .forEach(item -> {
                    Long serviceId = item.getSalonService().getId();
                    serviceCounts.merge(serviceId, 1L, Long::sum);
                    serviceNames.put(serviceId, item.getSalonService().getName());
                });

        List<PopularServiceResponse> popularServices = serviceCounts.entrySet().stream()
                .map(entry -> new PopularServiceResponse(
                        entry.getKey(),
                        serviceNames.get(entry.getKey()),
                        entry.getValue()
                ))
                .sorted(Comparator.comparingLong(PopularServiceResponse::requestCount)
                        .reversed()
                        .thenComparing(PopularServiceResponse::salonServiceName))
                .limit(5)
                .toList();

        return new WeeklyDashboardResponse(
                weekStart,
                weekEnd,
                appointments.size(),
                completedCount,
                canceledCount,
                revenue,
                popularServices
        );
    }

    @Transactional
    public AdminAppointmentResponse confirmAppointment(Long appointmentId) {
        Appointment appointment = findAppointment(appointmentId);
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new InvalidAppointmentStatusException("Only pending appointments can be confirmed.");
        }
        boolean allServicesCompleted = !appointment.getItems().isEmpty()
                && appointment.getItems().stream()
                .allMatch(item -> item.getStatus() == AppointmentItemStatus.COMPLETED);
        appointment.setStatus(
                allServicesCompleted ? AppointmentStatus.COMPLETED : AppointmentStatus.CONFIRMED
        );
        appointmentRepository.save(appointment);

        return toAdminAppointmentResponse(appointment);
    }

    @Transactional
    public AdminAppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = findAppointment(appointmentId);
        ensureEditable(appointment);
        appointment.setStatus(AppointmentStatus.CANCELED);
        return toAdminAppointmentResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AdminAppointmentResponse updateServiceStatus(
            Long appointmentId,
            Long itemId,
            AppointmentItemStatus status
    ) {
        Appointment appointment = findAppointment(appointmentId);
        ensureEditable(appointment);

        AppointmentItem appointmentItem = appointment.getItems().stream()
                .filter(item -> item.getId() == itemId)
                .findFirst()
                .orElseThrow(() ->
                        new AppointmentNotFoundException(
                                "Service item not found in appointment with ID: " + appointmentId
                        ));

        appointmentItem.setStatus(status);
        boolean allServicesCompleted = !appointment.getItems().isEmpty()
                && appointment.getItems().stream()
                .allMatch(item -> item.getStatus() == AppointmentItemStatus.COMPLETED);
        if (allServicesCompleted && appointment.getStatus() == AppointmentStatus.CONFIRMED) {
            appointment.setStatus(AppointmentStatus.COMPLETED);
        }
        appointmentRepository.save(appointment);

        return toAdminAppointmentResponse(appointment);
    }

    private Appointment findAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(
                        "Appointment not found with ID: " + appointmentId
                ));
    }

    private void ensureEditable(Appointment appointment) {
        if (appointment.getStatus() == AppointmentStatus.CANCELED
                || appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new InvalidAppointmentStatusException(
                    "Canceled or completed appointments cannot be changed."
            );
        }
    }

    private Optional<Appointment> findFirstActiveAppointmentInWeek(LocalDateTime dateTime, Long customerId) {
         LocalDate weekStart = dateTime.toLocalDate()
                 .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
         LocalDateTime startInclusive = weekStart.atStartOfDay();
         LocalDateTime endExclusive = weekStart.plusWeeks(1).atStartOfDay();

         return appointmentRepository.findFirstByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanAndStatusInOrderByDateTimeAsc(
                 customerId,
                 startInclusive,
                 endExclusive,
                 List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED)
         );
    }

    private AppointmentResponse toAppointmentResponse (Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDateTime(),
                appointment.getStatus(),
                toAppointmentItemResponses(appointment)
        );
    }

    private AdminAppointmentResponse toAdminAppointmentResponse(Appointment appointment) {
        return new AdminAppointmentResponse(
                appointment.getId(),
                appointment.getDateTime(),
                appointment.getStatus(),
                toAppointmentItemResponses(appointment),
                appointment.getCustomer().getName()
        );
    }

    private List<AppointmentItemResponse> toAppointmentItemResponses(Appointment appointment) {
        return appointment.getItems().stream()
                .map(item -> new AppointmentItemResponse(
                        item.getId(),
                        item.getSalonService().getId(),
                        item.getSalonService().getName(),
                        item.getPriceCharged(),
                        item.getStatus()
                ))
                .toList();
    }

    private void validateAppointmentSchedule(LocalDateTime dateTime) {
        boolean isSunday = dateTime.getDayOfWeek() == DayOfWeek.SUNDAY;
        boolean outsideHours = dateTime.getHour() < 9 || dateTime.getHour() > 16;
        boolean isNotWholeHour = dateTime.getMinute() != 0
                || dateTime.getSecond() != 0
                || dateTime.getNano() != 0;

        if (isSunday || outsideHours || isNotWholeHour) {
            throw new InvalidAppointmentScheduleException(
                    "Agendamentos devem ocorrer de segunda a sábado, entre 09:00 e 16:00, em horas cheias."
            );
        }
    }

    private List<SalonService> loadAndValidateActiveServices(List<Long> serviceIds) {
        Set<Long> uniqueIds = new HashSet<>(serviceIds);

        if (uniqueIds.size() != serviceIds.size()) {
            throw new DuplicateServiceException("Duplicate service found.");
        }

        List<SalonService> salonServices = salonServiceRepository.findAllById(uniqueIds);

        if (salonServices.size() != uniqueIds.size()) {
            throw new InvalidSalonServiceException("One or more services do not exist.");
        }

        for (SalonService salonService : salonServices) {
            if (!salonService.isActive()) {
                throw new InvalidSalonServiceException("One or more services are not active.");
            }
        }

        return salonServices;
    }
}