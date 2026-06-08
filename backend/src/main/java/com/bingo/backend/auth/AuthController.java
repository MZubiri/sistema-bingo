package com.bingo.backend.auth;

import com.bingo.backend.audit.AuditAction;
import com.bingo.backend.audit.AuditService;
import com.bingo.backend.auth.AuthDtos.LoginRequest;
import com.bingo.backend.auth.AuthDtos.LoginResponse;
import com.bingo.backend.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuditService auditService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.username()).orElse(null);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
            User authenticated = userRepository.findByUsername(request.username()).orElseThrow();
            auditService.register(null, authenticated, AuditAction.LOGIN_SUCCESS, "Inicio de sesion correcto");
            return new LoginResponse(
                    jwtService.generate(authenticated),
                    authenticated.getUsername(),
                    authenticated.getFullName(),
                    authenticated.getRole(),
                    authenticated.getOrganizationCode()
            );
        } catch (BadCredentialsException ex) {
            if (user != null) {
                auditService.register(null, user, AuditAction.LOGIN_FAILED, "Credenciales invalidas");
            }
            throw ex;
        }
    }
}
