package tn.star.star_api.repository;

import tn.star.star_api.entity.AssociationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface AssociationMemberRepository extends JpaRepository<AssociationMember, UUID> {
    Optional<AssociationMember> findByUserId(UUID userId);
    Optional<AssociationMember> findByCategoryId(Integer categoryId);
    boolean existsByUserId(UUID userId);
    boolean existsByCategoryId(Integer categoryId);
}
