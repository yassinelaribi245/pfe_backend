package tn.star.star_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.dto.LoginRequest;
import tn.star.star_api.dto.LoginResponse;
import tn.star.star_api.entity.User;
import tn.star.star_api.repository.UserRepository;
import tn.star.star_api.security.JwtUtil;
import tn.star.star_api.service.ActivityLogService;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtil           jwtUtil;
    private final ActivityLogService logService;

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(
                request.getPassword(), user.getPassword())) {
            // Log failed attempt (user might be null so pass null)
            if (user != null) {
                logService.log(ActivityLogService.LOGIN_FAILED, user,
                    "Email: " + request.getEmail()
                    + " — IP: " + getIp(httpRequest));
            }
            return ResponseEntity.status(401).body(
                Map.of("message", "Email ou mot de passe incorrect"));
        }

        if (user.getStatus() != User.UserStatus.active) {
            return ResponseEntity.status(403).body(
                Map.of("message", "Compte désactivé"));
        }

        String token = jwtUtil.generateToken(
                user.getEmail(), user.getRole().name());

        // Log successful login
        logService.log(ActivityLogService.LOGIN_SUCCESS, user,
            "Rôle: " + user.getRole().name()
            + " — IP: " + getIp(httpRequest));

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getEmail(),
                user.getName() + " " + user.getLastName(),
                user.getRole().name()
        ));
    }

    private String getIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return forwarded != null
            ? forwarded.split(",")[0].trim()
            : req.getRemoteAddr();
    }
}
