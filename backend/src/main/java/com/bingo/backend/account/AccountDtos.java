package com.bingo.backend.account;

import jakarta.validation.constraints.NotBlank;

public final class AccountDtos {
    private AccountDtos() {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
            @NotBlank(message = "La nueva contraseña es obligatoria") String newPassword,
            @NotBlank(message = "La confirmación es obligatoria") String confirmPassword
    ) {
    }

    public record MessageResponse(String message) {
    }
}
