package tn.star.star_api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offer")
@Data
@NoArgsConstructor
public class Offer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private OfferCategory category;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "max_participants", nullable = false)
    private Integer maxParticipants = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "document_needed", length = 255)
    private String documentNeeded;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "payment_method", nullable = false, columnDefinition = "payment_method_type")
    private PaymentMethod paymentMethod = PaymentMethod.free;

    // JSON array of allowed payment methods e.g. ["full","months_3","months_6"]
    // When offer is free this is always ["free"]
    @Column(name = "allowed_payment_methods", columnDefinition = "TEXT")
    private String allowedPaymentMethods = "[\"free\"]";

    public enum PaymentMethod {
        free, full, months_3, months_6, months_9, months_12;

        /** How many installments this method produces */
        public int installments() {
            return switch (this) {
                case free, full -> 1;
                case months_3   -> 3;
                case months_6   -> 6;
                case months_9   -> 9;
                case months_12  -> 12;
            };
        }

        /** Display label shown in the app */
        public String label() {
            return switch (this) {
                case free      -> "Gratuit";
                case full      -> "Paiement intégral";
                case months_3  -> "Paiement en 3 mensualités";
                case months_6  -> "Paiement en 6 mensualités";
                case months_9  -> "Paiement en 9 mensualités";
                case months_12 -> "Paiement en 12 mensualités";
            };
        }
    }

    // ── Binary image storage ──────────────────────────────
    // LAZY = only loaded when explicitly accessed (not in list queries)
    // This prevents loading MBs of image data for every offer in the list
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "cover_image", columnDefinition = "BYTEA")
    private byte[] coverImage;

    @Column(name = "cover_image_type", length = 20)
    private String coverImageType;

    // Gallery stored as JSON array of base64 strings in TEXT column
    @Column(name = "images", columnDefinition = "TEXT")
    private String images;

    @Column(name = "images_types", columnDefinition = "TEXT")
    private String imagesTypes;

    // Flag to know if cover exists without loading the binary
    // We derive this from coverImageType being non-null
    public boolean hasCoverImage() {
        return coverImageType != null;
    }

    public boolean hasGallery() {
        return images != null && !images.isEmpty();
    }

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "offer_status")
    private OfferStatus status = OfferStatus.active;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by")
    private AssociationMember createdBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_admin")
    private User createdByAdmin;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum OfferStatus {
        active, suggested, cancelled, finished, fermer
    }
}
