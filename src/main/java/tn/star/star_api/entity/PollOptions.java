package tn.star.star_api.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "poll_options",
    uniqueConstraints = @UniqueConstraint(columnNames = {"poll_id", "option"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PollOptions {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "poll_id", nullable = false)
    private Poll poll;

    @Column(nullable = false, length = 200)
    private String option;

    @Column(nullable = false)
    private Integer count = 0;
}
