package tn.star.star_api.repository;
import tn.star.star_api.entity.OfferRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface OfferRegistrationRepository extends JpaRepository<OfferRegistration, UUID> {
    List<OfferRegistration> findByUserId(UUID userId);
    List<OfferRegistration> findByOfferId(UUID offerId);
    boolean existsByUserIdAndOfferId(UUID userId, UUID offerId);
}
