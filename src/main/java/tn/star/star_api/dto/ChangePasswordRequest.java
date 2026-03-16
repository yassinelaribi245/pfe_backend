package tn.star.star_api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "L'ancien mot de passe est obligatoire")
    private String oldPassword;

    @NotBlank(message = "Le nouveau mot de passe est obligatoire")
    @Size(min = 6, max = 100, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String newPassword;
}
