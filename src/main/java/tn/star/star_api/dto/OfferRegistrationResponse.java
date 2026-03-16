package tn.star.star_api.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import tn.star.star_api.entity.OfferRegistration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class OfferRegistrationResponse {

    private UUID   id;
    private String userName;
    private String userEmail;
    private String offerTitle;
    private String offerCategory;
    private String status;
    private String statusMessage;  // clear message for the user
    private OffsetDateTime registeredAt;

    public static OfferRegistrationResponse from(OfferRegistration r) {
        String message;
        switch (r.getStatus()) {
            case confirmed ->
                message = "Votre inscription est confirmée ✅";
            case waitlist ->
                message = "Vous êtes sur liste d'attente — "
                    + "vous serez notifié si une place se libère 🕐";
            case cancelled ->
                message = "Inscription annulée";
            default ->
                message = "";
        }

        return new OfferRegistrationResponse(
            r.getId(),
            r.getUser().getName() + " " + r.getUser().getLastName(),
            r.getUser().getEmail(),
            r.getOffer().getTitle(),
            r.getOffer().getCategory() != null
                ? r.getOffer().getCategory().getName() : null,
            r.getStatus().name(),
            message,
            r.getDate()
        );
    }
}
