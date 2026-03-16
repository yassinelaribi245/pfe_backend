package tn.star.star_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "offer_registration",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_user", "id_offer"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_offer", nullable = false)
    private Offer offer;

    @Column(nullable = false)
    private OffsetDateTime date = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private RegistrationStatus status = RegistrationStatus.confirmed;

    public enum RegistrationStatus {
        confirmed, cancelled, waitlist
    }
}
