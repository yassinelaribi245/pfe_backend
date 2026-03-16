package tn.star.star_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "election_vote",
    uniqueConstraints = @UniqueConstraint(columnNames = {"election_id", "voter_id", "candidate_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElectionVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "election_id", nullable = false)
    private Elections election;

    @ManyToOne
    @JoinColumn(name = "voter_id", nullable = false)
    private User voter;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateApplication candidate;

    @Column(name = "voted_at")
    private OffsetDateTime votedAt = OffsetDateTime.now();
}
