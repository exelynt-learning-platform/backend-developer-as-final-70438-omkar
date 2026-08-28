package com.omkar.resourcebooking.config;

import com.omkar.resourcebooking.entity.Resource;
import com.omkar.resourcebooking.entity.Role;
import com.omkar.resourcebooking.entity.User;
import com.omkar.resourcebooking.repository.ResourceRepository;
import com.omkar.resourcebooking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            ResourceRepository resourceRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User("admin", passwordEncoder.encode("admin123"), Role.ADMIN);
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("user1")) {
            User user = new User("user1", passwordEncoder.encode("user123"), Role.USER);
            userRepository.save(user);
        }

        if (resourceRepository.count() == 0) {
            resourceRepository.save(new Resource("Conference Room A", "Large room with projector and 20 seats", "Room", true, new BigDecimal("150.00")));
            resourceRepository.save(new Resource("Executive Electric SUV", "Luxury electric vehicle for client transport", "Vehicle", true, new BigDecimal("300.00")));
            resourceRepository.save(new Resource("4K Cinema Camera Kit", "Professional recording camera with tripod and lenses", "Equipment", true, new BigDecimal("85.00")));
        }
    }
}
