package tn.star.star_api.repository;
import tn.star.star_api.entity.Elections;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
public interface ElectionsRepository extends JpaRepository<Elections, UUID> {
    List<Elections> findByStatus(Elections.ElectionStatus status);
}
