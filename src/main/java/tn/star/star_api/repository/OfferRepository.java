package tn.star.star_api.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.star.star_api.entity.Offer;

import java.util.List;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    List<Offer> findByStatus(Offer.OfferStatus status, Sort sort);

    List<Offer> findByCreatedById(UUID memberId, Sort sort);

    // Keep old methods for backward compat
    List<Offer> findByStatus(Offer.OfferStatus status);
    List<Offer> findByCreatedById(UUID memberId);
}
