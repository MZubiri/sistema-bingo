package com.bingo.backend.config;

import com.bingo.backend.auth.User;
import com.bingo.backend.auth.UserRepository;
import com.bingo.backend.common.OrganizationCode;
import com.bingo.backend.common.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(1)
@RequiredArgsConstructor
public class SeedDataInitializer implements CommandLineRunner {
    private static final String INITIAL_PASSWORD = "Bingo2026";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUser("admin", "Administrador General", Role.ADMIN, OrganizationCode.ADMIN, 3000);
        createUser("geourp01", "Representante GEOURP 01", Role.VENDEDOR, OrganizationCode.GEOURP, 1000);
        createUser("geourp02", "Representante GEOURP 02", Role.VENDEDOR, OrganizationCode.GEOURP, 1000);
        createUser("geourp03", "Representante GEOURP 03", Role.VENDEDOR, OrganizationCode.GEOURP, 1000);
        createUser("geourp04", "Representante GEOURP 04", Role.VENDEDOR, OrganizationCode.GEOURP, 1000);
        createUser("civial01", "Representante CIVIAL 01", Role.VENDEDOR, OrganizationCode.CIVIAL, 1000);
        createUser("civial02", "Representante CIVIAL 02", Role.VENDEDOR, OrganizationCode.CIVIAL, 1000);
        createUser("civial03", "Representante CIVIAL 03", Role.VENDEDOR, OrganizationCode.CIVIAL, 1000);
        createUser("civial04", "Representante CIVIAL 04", Role.VENDEDOR, OrganizationCode.CIVIAL, 1000);
        createUser("aci01", "Representante ACI 01", Role.VENDEDOR, OrganizationCode.ACI, 1000);
        createUser("aci02", "Representante ACI 02", Role.VENDEDOR, OrganizationCode.ACI, 1000);
        createUser("aci03", "Representante ACI 03", Role.VENDEDOR, OrganizationCode.ACI, 1000);
        createUser("aci04", "Representante ACI 04", Role.VENDEDOR, OrganizationCode.ACI, 1000);
    }

    private void createUser(String username, String fullName, Role role, OrganizationCode organizationCode, int quotaTotal) {
        var existing = userRepository.findByUsername(username);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getFullName().startsWith("Vendedor ")) {
                user.setFullName(fullName);
                user.setQuotaTotal(quotaTotal);
                userRepository.save(user);
            }
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setOrganizationCode(organizationCode);
        user.setQuotaTotal(quotaTotal);
        user.setQuotaUsed(0);
        user.setActive(true);
        user.setPasswordHash(passwordEncoder.encode(INITIAL_PASSWORD));
        userRepository.save(user);
    }
}
