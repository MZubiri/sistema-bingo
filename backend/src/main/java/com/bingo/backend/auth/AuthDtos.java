package com.bingo.backend.auth;

import com.bingo.backend.common.OrganizationCode;
import com.bingo.backend.common.Role;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {
    }

    public record LoginRequest(
            @NotBlank(message = "El usuario es obligatorio") String username,
            @NotBlank(message = "La contraseña es obligatoria") String password
    ) {
    }

    public record LoginResponse(
            String token,
            String username,
            String fullName,
            Role role,
            OrganizationCode organizationCode
    ) {
    }
}
