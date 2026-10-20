package com.pvmarcon.cabeleleila.controller;

import com.pvmarcon.cabeleleila.dto.AppointmentResponse;
import com.pvmarcon.cabeleleila.dto.ServiceResponse;
import com.pvmarcon.cabeleleila.service.SalonCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/services")
public class ServiceController {

    private final SalonCatalogService salonCatalogService;

    public ServiceController(SalonCatalogService salonCatalogService) {
        this.salonCatalogService = salonCatalogService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceResponse>> getActiveServices() {
        List<ServiceResponse> response = salonCatalogService.getActiveServices();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
