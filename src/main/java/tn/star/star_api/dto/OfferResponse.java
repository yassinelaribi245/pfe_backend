package tn.star.star_api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.AllArgsConstructor;
import tn.star.star_api.entity.Offer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OfferResponse {

    private UUID     id;
    private String   title;
    private String   description;
    private String   category;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer  maxParticipants;
    private BigDecimal price;
    private String   documentNeeded;
    private String   paymentMethod;
    private String   status;
    private String   coverImage;
    private List<String> images;
    private String   responsibleMember;
    private Boolean  createdByAdmin;
    private String   adminName;
    private OffsetDateTime createdAt;

    @SuppressWarnings("unchecked")
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

        // Parse images JSON array
        List<String> imageList = null;
        if (o.getImages() != null && !o.getImages().isEmpty()) {
            try {
                imageList = new ObjectMapper().readValue(
                    o.getImages(), List.class);
            } catch (Exception ignored) {}
        }

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
            o.getCoverImage(),
            imageList,
            memberName,
            adminCreated,
            adminName,
            o.getCreatedAt()
        );
    }
}
