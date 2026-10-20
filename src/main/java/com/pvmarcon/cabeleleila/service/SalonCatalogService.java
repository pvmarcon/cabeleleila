package com.pvmarcon.cabeleleila.service;

import com.pvmarcon.cabeleleila.dto.ServiceResponse;
import com.pvmarcon.cabeleleila.repository.SalonServiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SalonCatalogService {

    private final SalonServiceRepository salonServiceRepository;

    public SalonCatalogService(SalonServiceRepository salonServiceRepository) {
        this.salonServiceRepository = salonServiceRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getActiveServices() {
        return salonServiceRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(service -> new ServiceResponse(
                        service.getId(),
                        service.getName(),
                        service.getDescription(),
                        service.getDurationMinutes(),
                        service.getPrice()
                ))
                .toList();
    }
}
