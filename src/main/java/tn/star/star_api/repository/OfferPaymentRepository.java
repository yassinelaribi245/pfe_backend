package tn.star.star_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.star.star_api.entity.OfferPayment;

import java.util.List;
import java.util.UUID;

public interface OfferPaymentRepository extends JpaRepository<OfferPayment, UUID> {

    List<OfferPayment> findByRegistrationId(UUID registrationId);

    // All payments for every registration of a given offer — ordered by name then installment
    @Query(value = """
        SELECT op.* FROM offer_payment op
        JOIN offer_registration r ON r.id = op.registration_id
        JOIN users u ON u.id = r.id_user
        WHERE r.id_offer = :offerId
        ORDER BY u.last_name, op.installment_number
        """, nativeQuery = true)
    List<OfferPayment> findByOfferId(@Param("offerId") UUID offerId);

    // Check if a user has ANY overdue payments across all offers
    @Query(value = """
        SELECT COUNT(op.id) > 0
        FROM offer_payment op
        JOIN offer_registration r ON r.id = op.registration_id
        WHERE r.id_user = :userId
          AND op.status = 'overdue'::payment_status
        """, nativeQuery = true)
    boolean userHasOverduePayments(@Param("userId") UUID userId);

    // Count unpaid (pending or overdue) installments for a registration
    @Query(value = """
        SELECT COUNT(op.id)
        FROM offer_payment op
        WHERE op.registration_id = :registrationId
          AND op.status <> 'paid'::payment_status
        """, nativeQuery = true)
    long countUnpaidByRegistrationId(@Param("registrationId") UUID registrationId);
}
