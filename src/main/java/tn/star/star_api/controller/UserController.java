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
    public ResponseEntity<?> createUser(
            @Valid @RequestBody CreateUserRequest req) {
        return ResponseEntity.ok(userService.createUser(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest req) {
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    // PATCH /api/users/{id}/points — update points only
    @PatchMapping("/{id}/points")
    public ResponseEntity<?> updatePoints(@PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> body) {
        var req = new UpdateUserRequest();
        if (body.get("credit") != null) {
            req.setCredit(new java.math.BigDecimal(body.get("credit").toString()));
        }
        return ResponseEntity.ok(userService.updateUser(id, req));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable UUID id,
            @Valid @RequestBody ChangeRoleRequest req) {
        return ResponseEntity.ok(userService.changeRole(id, req));
    }

    // PATCH /api/users/{id}/responsibilities
    // Adds additional category responsibilities to an existing member
    // without changing their role
    @PatchMapping("/{id}/responsibilities")
    public ResponseEntity<?> addResponsibilities(@PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> body) {
        return ResponseEntity.ok(
            userService.addResponsibilities(id, body));
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
