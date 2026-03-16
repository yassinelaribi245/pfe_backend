package tn.star.star_api.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OfferRequest {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 200, message = "Le titre doit contenir entre 3 et 200 caractères")
    private String title;

    @NotBlank(message = "La description est obligatoire")
    private String description;

    // Only required when admin creates an offer
    // Association members don't send this — it's auto-assigned from their profile
    private Integer categoryId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Le nombre de places est obligatoire")
    @Min(value = 1, message = "Le nombre de places doit être au moins 1")
    private Integer maxParticipants;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", message = "Le prix ne peut pas être négatif")
    private BigDecimal price;

    @Size(max = 255, message = "La description du document ne doit pas dépasser 255 caractères")
    private String documentNeeded;

    @NotBlank(message = "Le mode de paiement est obligatoire")
    private String paymentMethod;
}
