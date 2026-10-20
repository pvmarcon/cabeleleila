package com.pvmarcon.cabeleleila.repository;

import com.pvmarcon.cabeleleila.model.Appointment;
import com.pvmarcon.cabeleleila.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findFirstByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanAndStatusInOrderByDateTimeAsc(
            Long customerId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            List<AppointmentStatus> statuses
    );

    List<Appointment> findByCustomer_IdAndDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
            Long customerId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    List<Appointment> findAllByOrderByDateTimeAsc();

    List<Appointment> findAllByDateTimeGreaterThanEqualAndDateTimeLessThanOrderByDateTimeAsc(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );
}