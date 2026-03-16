package tn.star.star_api.repository;
import tn.star.star_api.entity.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface OfferRepository extends JpaRepository<Offer, UUID> {
    List<Offer> findByStatus(Offer.OfferStatus status);
    List<Offer> findByCreatedById(UUID memberId);
}
