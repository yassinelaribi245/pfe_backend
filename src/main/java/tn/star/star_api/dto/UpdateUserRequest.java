package tn.star.star_api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import tn.star.star_api.entity.User;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Le prénom doit contenir entre 2 et 100 caractères")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$", message = "Le prénom contient des caractères invalides")
    private String name;

    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s'-]+$", message = "Le nom contient des caractères invalides")
    private String lastName;

    @Pattern(regexp = "^[+0-9\\s()-]{7,20}$",
             message = "Numéro de téléphone invalide")
    private String phone;

    private User.UserRole role;
    private User.UserStatus status;
}
