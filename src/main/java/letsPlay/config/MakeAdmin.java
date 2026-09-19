package letsPlay.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import letsPlay.enums.Role;
import letsPlay.models.UserModel;
import letsPlay.repository.UserRepository;

@Configuration
public class MakeAdmin {
    private static final Logger log = LoggerFactory.getLogger(MakeAdmin.class);

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;

    @Bean
    public CommandLineRunner makeAdminSeeder() {
        return args -> {
            try {
                if (userRepository.existsByName("admin")) {
                    return;
                }

                UserModel admin = new UserModel();
                admin.setName("admin");
                admin.setEmail("admin@gmail.com");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole(Role.ADMIN);

                userRepository.save(admin);
                log.info(">> Admin user created successfully (username: admin)!");
            } catch (Exception e) {
                // Transient DB unavailability at startup must not crash the app.
                // The seeder will create the admin on the next successful startup.
                log.warn(">> Admin seeder skipped: database not reachable at startup ({})", e.getMessage());
            }
        };
    }
}