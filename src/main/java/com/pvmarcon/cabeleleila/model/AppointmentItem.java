package com.pvmarcon.cabeleleila.model;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "appointment_items")
public class AppointmentItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salon_service_id", nullable = false)
    private SalonService salonService;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal priceCharged;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AppointmentItemStatus status = AppointmentItemStatus.PENDING;

    public AppointmentItem() {
    }

    public AppointmentItem(SalonService salonService, BigDecimal priceCharged) {
        this.salonService = salonService;
        this.priceCharged = priceCharged;
    }

    public long getId() {
        return id;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment appointment) {
        this.appointment = appointment;
    }

    public SalonService getSalonService() {
        return salonService;
    }

    public void setSalonService(SalonService salonService) {
        this.salonService = salonService;
    }

    public BigDecimal getPriceCharged() {
        return priceCharged;
    }

    public void setPriceCharged(BigDecimal priceCharged) {
        this.priceCharged = priceCharged;
    }

    public AppointmentItemStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentItemStatus status) {
        this.status = status;
    }
}
