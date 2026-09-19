package letsPlay.config;

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
                String admin_password = System.getenv("ADMIN_PASSWORD");
                admin.setPassword(passwordEncoder.encode(admin_password));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                System.out.println(">> Admin user created successfully");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        };
    }
}