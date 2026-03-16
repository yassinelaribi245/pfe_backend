package tn.star.star_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.star.star_api.dto.*;
import tn.star.star_api.service.UserService;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(Authentication auth) {
        return ResponseEntity.ok(userService.getMyProfile(auth.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(userService.createUser(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id,
                                        @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Utilisateur supprimé");
    }

    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication auth,
                                            @Valid @RequestBody ChangePasswordRequest req) {
        userService.changePassword(auth.getName(), req);
        return ResponseEntity.ok("Mot de passe modifié avec succès");
    }
}
