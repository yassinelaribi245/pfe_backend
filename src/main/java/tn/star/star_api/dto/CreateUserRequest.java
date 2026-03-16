package tn.star.star_api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import tn.star.star_api.entity.User;

@Data
public class CreateUserRequest {

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$", message = "Le prénom contient des caractères invalides")
    private String name;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$", message = "Le nom contient des caractères invalides")
    private String lastName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    @Size(max = 150, message = "L'email ne doit pas dépasser 150 caractères")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, max = 100, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[+0-9\\s()-]{7,20}$", message = "Numéro de téléphone invalide")
    private String phone;

    @NotNull(message = "Le rôle est obligatoire")
    private User.UserRole role;

    // Only required when role = association_member
    // Must be one of the 12 fixed categories (1-12)
    private Integer categoryId;
}
