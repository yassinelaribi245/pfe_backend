package tn.star.star_api.repository;
import tn.star.star_api.entity.PollOptions;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface PollOptionsRepository extends JpaRepository<PollOptions, UUID> {
    List<PollOptions> findByPollId(UUID pollId);
}
