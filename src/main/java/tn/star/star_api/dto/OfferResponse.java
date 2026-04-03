package tn.star.star_api.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.AllArgsConstructor;
import tn.star.star_api.entity.Offer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OfferResponse {

    private UUID        id;
    private String      title;
    private String      description;
    private String      category;
    private LocalDate   startDate;
    private LocalDate   endDate;
    private Integer     maxParticipants;
    private BigDecimal  price;
    private String      documentNeeded;
    private String      paymentMethod;      // enum name e.g. "months_3"
    private String      paymentMethodLabel; // human label e.g. "Paiement en 3 mensualités"
    private List<String> allowedPaymentMethods; // e.g. ["full","months_3","months_6"]
    private String      status;

    // Image URLs pointing to the DB-serving endpoints
    private String       coverImage;   // /api/images/{id}/cover
    private List<String> images;       // /api/images/{id}/gallery/0 ...

    private String   responsibleMember;
    private Boolean  createdByAdmin;
    private String   adminName;
    private OffsetDateTime createdAt;

    @SuppressWarnings("unchecked")
    private static List<String> parseAllowedMethods(String json) {
        if (json == null || json.isEmpty()) return List.of("free");
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>(){});
        } catch (Exception e) { return List.of("free"); }
    }

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

        // Build image URLs — don't access binary data here,
        // just check if images exist using the type/metadata fields
        String coverUrl = null;
        if (o.hasCoverImage()) {
            coverUrl = "/api/images/" + o.getId() + "/cover";
        }

        List<String> galleryUrls = new ArrayList<>();
        if (o.hasGallery()) {
            try {
                List<?> list = new ObjectMapper()
                    .readValue(o.getImages(), List.class);
                for (int i = 0; i < list.size(); i++) {
                    galleryUrls.add("/api/images/" + o.getId()
                        + "/gallery/" + i);
                }
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
            o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "free",
            o.getPaymentMethod() != null ? o.getPaymentMethod().label() : "Gratuit",
            parseAllowedMethods(o.getAllowedPaymentMethods()),
            o.getStatus().name(),
            coverUrl,
            galleryUrls.isEmpty() ? null : galleryUrls,
            memberName,
            adminCreated,
            adminName,
            o.getCreatedAt()
        );
    }
}
