package tn.star.star_api.repository;
import tn.star.star_api.entity.CandidatesValider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface CandidatesValiderRepository extends JpaRepository<CandidatesValider, UUID> {
    List<CandidatesValider> findByElectionId(UUID electionId);
}
