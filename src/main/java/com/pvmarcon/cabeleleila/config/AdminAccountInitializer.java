package com.pvmarcon.cabeleleila.config;

import com.pvmarcon.cabeleleila.model.Profile;
import com.pvmarcon.cabeleleila.model.User;
import com.pvmarcon.cabeleleila.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword
    )
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!userRepository.existsByEmail(adminEmail)) {
            var admin = new User(
                    "Leila",
                    adminEmail,
                    "",
                    passwordEncoder.encode(adminPassword),
                    Profile.ADMIN
            );
            userRepository.save(admin);
        }
    }
}
