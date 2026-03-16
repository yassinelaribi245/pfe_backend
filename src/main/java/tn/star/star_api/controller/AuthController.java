package tn.star.star_api.controller;

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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(401)
                    .body("Email ou mot de passe incorrect");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401)
                    .body("Email ou mot de passe incorrect");
        }

        if (user.getStatus() != User.UserStatus.active) {
            return ResponseEntity.status(403)
                    .body("Compte désactivé");
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name()
        );

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getEmail(),
                user.getName() + " " + user.getLastName(),
                user.getRole().name()
        ));
    }
}
