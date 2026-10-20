package com.pvmarcon.cabeleleila.repository;

import com.pvmarcon.cabeleleila.model.SalonService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SalonServiceRepository extends JpaRepository<SalonService, Long> {
    List<SalonService> findByActiveTrueOrderByNameAsc();
}
