package tn.star.star_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.star.star_api.entity.User;

@Data
public class ChangeRoleRequest {

    @NotNull(message = "Le nouveau rôle est obligatoire")
    private User.UserRole newRole;

    // Required only when newRole = association_member
    private Integer categoryId;
}
