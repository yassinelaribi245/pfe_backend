package tn.star.star_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.star.star_api.dto.*;
import tn.star.star_api.entity.*;
import tn.star.star_api.repository.*;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository              userRepository;
    private final AssociationMemberRepository memberRepository;
    private final OfferCategoryRepository     categoryRepository;
    private final PasswordEncoder             passwordEncoder;
    private final ActivityLogService          logService;

    // ── Get all users ─────────────────────────────────────
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream().map(UserResponse::from).toList();
    }

    // ── Get one user ──────────────────────────────────────
    public UserResponse getUserById(UUID id) {
        return UserResponse.from(userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Utilisateur introuvable")));
    }

    // ── Get my profile ────────────────────────────────────
    public UserResponse getMyProfile(String email) {
        return UserResponse.from(userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                    "Utilisateur introuvable")));
    }

    // ── Create user ───────────────────────────────────────
    @Transactional
    public UserResponse createUser(CreateUserRequest req) {

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email déjà utilisé");
        }

        if (req.getRole() == User.UserRole.association_member) {
            if (req.getCategoryId() == null) {
                throw new RuntimeException(
                    "Un membre association doit avoir une catégorie assignée");
            }
            OfferCategory category = categoryRepository
                    .findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                        "Catégorie introuvable"));
            if (memberRepository.existsByCategoryId(req.getCategoryId())) {
                throw new RuntimeException(
                    "La catégorie \"" + category.getName()
                    + "\" est déjà assignée à un autre membre");
            }
        }

        User user = new User();
        user.setName(req.getName());
        user.setLastName(req.getLastName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());
        user.setRole(req.getRole());
        user.setStatus(User.UserStatus.active);
        User saved = userRepository.save(user);

        if (req.getRole() == User.UserRole.association_member) {
            OfferCategory category = categoryRepository
                    .findById(req.getCategoryId()).get();
            AssociationMember member = new AssociationMember();
            member.setUser(saved);
            member.setCategory(category);
            memberRepository.save(member);
        }

        logService.log(ActivityLogService.USER_CREATED, saved,
            "Nouvel utilisateur: " + saved.getName() + " "
            + saved.getLastName() + " — Rôle: "
            + saved.getRole().name());
        return UserResponse.from(saved);
    }

    // ── Update user info (name, phone, status) ────────────
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest req) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Utilisateur introuvable"));

        if (req.getName()     != null) user.setName(req.getName());
        if (req.getLastName() != null) user.setLastName(req.getLastName());
        if (req.getPhone()    != null) user.setPhone(req.getPhone());
        if (req.getStatus()   != null) user.setStatus(req.getStatus());
        if (req.getCredit()   != null) user.setCredit(req.getCredit());

        return UserResponse.from(userRepository.save(user));
    }

    // ── Change role ───────────────────────────────────────
    // Handles all role transitions with category logic
    @Transactional
    public UserResponse changeRole(UUID id, ChangeRoleRequest req) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Utilisateur introuvable"));

        User.UserRole oldRole = user.getRole();
        User.UserRole newRole = req.getNewRole();

        if (oldRole == newRole) {
            throw new RuntimeException(
                "L'utilisateur a déjà ce rôle");
        }

        // ── Leaving association_member → free their category ──
        if (oldRole == User.UserRole.association_member) {
            memberRepository.findByUserId(id).ifPresent(m -> {
                // Their offers stay — just remove the member record
                // Offers will have createdBy pointing to deleted member
                // but category stays so next member can manage them
                memberRepository.delete(m);
            });
        }

        // ── Becoming association_member → need a free category ──
        if (newRole == User.UserRole.association_member) {
            if (req.getCategoryId() == null) {
                throw new RuntimeException(
                    "Vous devez choisir une catégorie pour ce membre");
            }
            OfferCategory category = categoryRepository
                    .findById(req.getCategoryId())
                    .orElseThrow(() -> new RuntimeException(
                        "Catégorie introuvable"));

            if (memberRepository.existsByCategoryId(req.getCategoryId())) {
                throw new RuntimeException(
                    "La catégorie \"" + category.getName()
                    + "\" est déjà assignée à un autre membre. "
                    + "Retirez d'abord ce membre de sa catégorie.");
            }

            // Update role first
            user.setRole(newRole);
            User saved = userRepository.save(user);

            // Create member record
            AssociationMember member = new AssociationMember();
            member.setUser(saved);
            member.setCategory(category);
            memberRepository.save(member);

            logService.log(ActivityLogService.ROLE_CHANGED, saved,
                saved.getName() + " " + saved.getLastName()
                + " : " + oldRole.name() + " → " + newRole.name()
                + " — catégorie: " + category.getName());
            return UserResponse.from(saved);
        }

        // For all other role changes just update the role
        user.setRole(newRole);
        UserResponse result = UserResponse.from(userRepository.save(user));
        logService.log(ActivityLogService.ROLE_CHANGED, user,
            user.getName() + " " + user.getLastName()
            + " : " + oldRole.name() + " → " + newRole.name());
        return result;
    }

    // ── Delete user ───────────────────────────────────────
    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                    "Utilisateur introuvable"));

        // If member — free their category first
        if (user.getRole() == User.UserRole.association_member) {
            memberRepository.findByUserId(id)
                    .ifPresent(memberRepository::delete);
        }

        String fullName = user.getName() + " " + user.getLastName();
        userRepository.deleteById(id);
        logService.log(ActivityLogService.USER_DELETED, null,
            "Utilisateur supprimé: " + fullName
            + " (" + user.getEmail() + ")"
            + " — Rôle: " + user.getRole().name());
    }

    // ── Change password ───────────────────────────────────
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                    "Utilisateur introuvable"));

        if (!passwordEncoder.matches(req.getOldPassword(),
                user.getPassword())) {
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
