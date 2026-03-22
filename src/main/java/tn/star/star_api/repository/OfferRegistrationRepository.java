package tn.star.star_api.repository;
import tn.star.star_api.entity.OfferRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
public interface OfferRegistrationRepository extends JpaRepository<OfferRegistration, UUID> {
    List<OfferRegistration> findByUserId(UUID userId);
    List<OfferRegistration> findByOfferId(UUID offerId);
    boolean existsByUserIdAndOfferId(UUID userId, UUID offerId);

    @Transactional
    void deleteByOfferId(UUID offerId);
}
