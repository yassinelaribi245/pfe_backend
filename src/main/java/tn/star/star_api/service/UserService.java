package tn.star.star_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.star.star_api.dto.*;
import tn.star.star_api.entity.*;
import tn.star.star_api.repository.*;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository             userRepository;
    private final AssociationMemberRepository memberRepository;
    private final OfferCategoryRepository    categoryRepository;
    private final PasswordEncoder            passwordEncoder;

    // ── Get all users ─────────────────────────────────────
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    // ── Get one user by id ────────────────────────────────
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return UserResponse.from(user);
    }

    // ── Get my own profile ────────────────────────────────
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        return UserResponse.from(user);
    }

    // ── Admin creates a user ──────────────────────────────
    public UserResponse createUser(CreateUserRequest req) {

        // Check email not already used
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        // If role is association_member, categoryId is required
        if (req.getRole() == User.UserRole.association_member) {
            if (req.getCategoryId() == null) {
                throw new RuntimeException(
                    "Un membre association doit avoir une catégorie assignée");
            }

            // Check category exists (must be 1-12)
            OfferCategory category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                        "Catégorie introuvable. Choisissez une catégorie entre 1 et 12"));

            // Check category not already assigned to another member
            if (memberRepository.existsByCategoryId(req.getCategoryId())) {
                throw new RuntimeException(
                    "La catégorie \"" + category.getName()
                    + "\" est déjà assignée à un autre membre");
            }
        }

        // Create the user
        User user = new User();
        user.setName(req.getName());
        user.setLastName(req.getLastName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setRole(req.getRole());
        user.setStatus(User.UserStatus.active);
        User saved = userRepository.save(user);

        // If association_member → create the member record with category
        if (req.getRole() == User.UserRole.association_member) {
            OfferCategory category = categoryRepository
                    .findById(req.getCategoryId()).get();
            AssociationMember member = new AssociationMember();
            member.setUser(saved);
            member.setCategory(category);
            memberRepository.save(member);
        }

        return UserResponse.from(saved);
    }

    // ── Admin updates a user ──────────────────────────────
    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
        if (req.getName()     != null) user.setName(req.getName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhone()    != null) user.setPhone(req.getPhone());
        if (req.getRole()     != null) user.setRole(req.getRole());
        if (req.getStatus()   != null) user.setStatus(req.getStatus());
        return UserResponse.from(userRepository.save(user));
    }

    // ── Admin deletes a user ──────────────────────────────
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur introuvable");
        }
        userRepository.deleteById(id);
    }

    // ── Any user changes their own password ───────────────
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Ancien mot de passe incorrect");
        }

        if (req.getOldPassword().equals(req.getNewPassword())) {
            throw new RuntimeException(
                "Le nouveau mot de passe doit être différent de l'ancien");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }
}
