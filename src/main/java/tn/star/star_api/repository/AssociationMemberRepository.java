package tn.star.star_api.repository;

import tn.star.star_api.entity.AssociationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssociationMemberRepository
        extends JpaRepository<AssociationMember, UUID> {

    // A user can manage multiple categories — return all their memberships
    List<AssociationMember> findByUserId(UUID userId);

    // Multiple members can share a category — return the list
    List<AssociationMember> findByCategoryId(Integer categoryId);

    // Count members in a category (max 4 allowed by business rule)
    long countByCategoryId(Integer categoryId);

    // Check if a user is already a member for a specific category
    boolean existsByUserIdAndCategoryId(UUID userId, Integer categoryId);

    boolean existsByUserId(UUID userId);

    // Keep for backward compat — returns first member of category if any
    Optional<AssociationMember> findFirstByCategoryId(Integer categoryId);
}
