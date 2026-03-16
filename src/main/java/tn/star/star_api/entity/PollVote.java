package tn.star.star_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "poll_vote",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_user", "id_option"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PollVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "id_user", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "id_option", nullable = false)
    private PollOptions option;

    @Column(name = "voted_at")
    private OffsetDateTime votedAt = OffsetDateTime.now();
}
