package tn.star.star_api.repository;

import tn.star.star_api.entity.OfferRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRegistrationRepository
        extends JpaRepository<OfferRegistration, UUID> {

    List<OfferRegistration> findByUserId(UUID userId);
    List<OfferRegistration> findByOfferId(UUID offerId);
    boolean existsByUserIdAndOfferId(UUID userId, UUID offerId);

    Optional<OfferRegistration> findByUserIdAndOfferId(UUID userId, UUID offerId);

    // All pending_approval registrations for a specific offer
    @Query(value = """
        SELECT * FROM offer_registration
        WHERE id_offer = :offerId
          AND status = 'pending_approval'::registration_status
        """, nativeQuery = true)
    List<OfferRegistration> findPendingApprovalByOfferId(@Param("offerId") UUID offerId);

    // All confirmed registrations for an offer (used for payment generation)
    @Query(value = """
        SELECT * FROM offer_registration
        WHERE id_offer = :offerId
          AND status = 'confirmed'::registration_status
        """, nativeQuery = true)
    List<OfferRegistration> findConfirmedByOfferId(@Param("offerId") UUID offerId);

    @Transactional
    void deleteByOfferId(UUID offerId);
}
