package com.lavander.estore.config;

import com.lavander.estore.model.Role;
import com.lavander.estore.model.User;
import com.lavander.estore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Keeps the fixed admin account's credentials in sync with config on every boot — to
// rotate the admin password, change ADMIN_PASSWORD and redeploy, no separate reset flow.
@Component
public class AdminAccountSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin = userRepository.findByEmail(adminEmail).orElseGet(() -> {
            User created = new User();
            created.setEmail(adminEmail);
            created.setFullName("Admin");
            return created;
        });
        admin.setRole(Role.ADMIN);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        userRepository.save(admin);
    }
}
