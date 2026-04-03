package tn.star.star_api.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.star.star_api.entity.Offer;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    List<Offer> findByStatus(Offer.OfferStatus status, Sort sort);
    List<Offer> findByStatus(Offer.OfferStatus status);
    List<Offer> findByCreatedById(UUID memberId, Sort sort);
    List<Offer> findByCreatedById(UUID memberId);

    // Offers that are still active but whose end_date has passed — for the scheduler
    @Query(value = """
        SELECT * FROM offer
        WHERE status = 'active'::offer_status
          AND end_date IS NOT NULL
          AND end_date <= :today
        """, nativeQuery = true)
    List<Offer> findExpiredActiveOffers(@Param("today") LocalDate today);
}
