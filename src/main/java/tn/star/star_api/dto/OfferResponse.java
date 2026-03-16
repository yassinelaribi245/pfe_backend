package tn.star.star_api.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import tn.star.star_api.entity.Offer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OfferResponse {

    private UUID       id;
    private String     title;
    private String     description;
    private String     category;
    private LocalDate  startDate;
    private LocalDate  endDate;
    private Integer    maxParticipants;
    private BigDecimal price;
    private String     documentNeeded;
    private String     paymentMethod;
    private String     status;

    // Member responsible for this category
    private String responsibleMember;

    // ── Admin tracking fields ─────────────────────────────
    // true  = this offer was created by an admin on behalf of the member
    // false = the member created it themselves
    private Boolean createdByAdmin;

    // Name of the admin who created it (only present if createdByAdmin = true)
    private String adminName;

    private OffsetDateTime createdAt;

    public static OfferResponse from(Offer o) {
        boolean adminCreated = o.getCreatedByAdmin() != null;

        String adminName = adminCreated
            ? o.getCreatedByAdmin().getName() + " "
              + o.getCreatedByAdmin().getLastName()
            : null;

        String memberName = o.getCreatedBy() != null
            ? o.getCreatedBy().getUser().getName() + " "
              + o.getCreatedBy().getUser().getLastName()
            : null;

        return new OfferResponse(
            o.getId(),
            o.getTitle(),
            o.getDescription(),
            o.getCategory() != null ? o.getCategory().getName() : null,
            o.getStartDate(),
            o.getEndDate(),
            o.getMaxParticipants(),
            o.getPrice(),
            o.getDocumentNeeded(),
            o.getPaymentMethod(),
            o.getStatus().name(),
            memberName,
            adminCreated,
            adminName,
            o.getCreatedAt()
        );
    }
}
