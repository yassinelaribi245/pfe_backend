package tn.star.star_api.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import tn.star.star_api.entity.User;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class UserResponse {
    private UUID id;
    private String name;
    private String lastName;
    private String email;
    private String role;
    private String phone;
    private BigDecimal credit;
    private String status;
    private OffsetDateTime createdAt;

    public static UserResponse from(User u) {
        return new UserResponse(
            u.getId(),
            u.getName(),
            u.getLastName(),
            u.getEmail(),
            u.getRole().name(),
            u.getPhone(),
            u.getCredit(),
            u.getStatus().name(),
            u.getCreatedAt()
        );
    }
}
