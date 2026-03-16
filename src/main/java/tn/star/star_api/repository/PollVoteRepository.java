package tn.star.star_api.repository;
import tn.star.star_api.entity.PollVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface PollVoteRepository extends JpaRepository<PollVote, UUID> {
    boolean existsByUserIdAndOptionPollId(UUID userId, UUID pollId);
}
