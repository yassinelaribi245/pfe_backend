package tn.star.star_api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.star.star_api.entity.User;
import java.util.List;

@Data
public class ChangeRoleRequest {

    @NotNull(message = "Le nouveau rôle est obligatoire")
    private User.UserRole newRole;

    // Kept for backward compat (single category)
    private Integer categoryId;

    // New: multiple categories — used when assigning/adding member responsibilities
    private List<Integer> categoryIds;

    // Helper: get effective list of category IDs
    public List<Integer> getEffectiveCategoryIds() {
        if (categoryIds != null && !categoryIds.isEmpty()) return categoryIds;
        if (categoryId  != null) return List.of(categoryId);
        return List.of();
    }
}
