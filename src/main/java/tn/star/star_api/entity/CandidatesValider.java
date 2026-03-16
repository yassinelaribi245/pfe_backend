package tn.star.star_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "candidates_valider",
    uniqueConstraints = @UniqueConstraint(columnNames = {"election_id", "application_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CandidatesValider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)
    private Elections election;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private CandidateApplication application;

    @ManyToOne
    @JoinColumn(name = "validated_by")
    private User validatedBy;

    @Column(name = "validated_at")
    private OffsetDateTime validatedAt = OffsetDateTime.now();
}
