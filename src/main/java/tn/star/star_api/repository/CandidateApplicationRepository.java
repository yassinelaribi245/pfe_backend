package tn.star.star_api.repository;
import tn.star.star_api.entity.CandidateApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface CandidateApplicationRepository extends JpaRepository<CandidateApplication, UUID> {
    List<CandidateApplication> findByElectionId(UUID electionId);
    List<CandidateApplication> findByUserId(UUID userId);
    boolean existsByElectionIdAndUserId(UUID electionId, UUID userId);
}
