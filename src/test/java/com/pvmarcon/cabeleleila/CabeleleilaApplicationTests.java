package com.pvmarcon.cabeleleila;

import com.pvmarcon.cabeleleila.model.SalonService;
import com.pvmarcon.cabeleleila.repository.SalonServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
		"app.admin.email=test-admin@example.test",
		"app.admin.password=test-only-password",
		"spring.datasource.url=jdbc:h2:mem:cabeleleila-test;DB_CLOSE_DELAY=-1"
})
class CabeleleilaApplicationTests {

	@Autowired
	private SalonServiceRepository salonServiceRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void seedsServicesWithPortugueseNames() {
		Set<String> serviceNames = salonServiceRepository.findAll().stream()
				.map(SalonService::getName)
				.collect(Collectors.toSet());

		assertEquals(Set.of("Corte de cabelo", "Coloração", "Escova"), serviceNames);
	}

}
