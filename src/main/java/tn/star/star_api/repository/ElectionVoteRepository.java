package tn.star.star_api.repository;
import tn.star.star_api.entity.ElectionVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface ElectionVoteRepository extends JpaRepository<ElectionVote, UUID> {
    List<ElectionVote> findByElectionId(UUID electionId);
    boolean existsByElectionIdAndVoterId(UUID electionId, UUID voterId);
    long countByCandidateId(UUID candidateId);
}
