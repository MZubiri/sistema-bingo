package com.bingo.backend.account;

import com.bingo.backend.account.AccountDtos.ChangePasswordRequest;
import com.bingo.backend.account.AccountDtos.MessageResponse;
import com.bingo.backend.audit.AuditAction;
import com.bingo.backend.audit.AuditService;
import com.bingo.backend.auth.User;
import com.bingo.backend.auth.UserRepository;
import com.bingo.backend.common.ApiException;
import com.bingo.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @PostMapping("/change-password")
    public MessageResponse changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        User user = principal.user();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La nueva contraseña y la confirmación no coinciden");
        }
        validatePassword(request.newPassword());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        auditService.register(null, user, AuditAction.PASSWORD_CHANGED, "Cambio de contraseña");
        return new MessageResponse("Contraseña actualizada correctamente");
    }

    private void validatePassword(String password) {
        if (password.length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe tener al menos 8 caracteres");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe incluir letras y números");
        }
    }
}
