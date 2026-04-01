package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.RewardCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RewardCatalogItemRepository extends JpaRepository<RewardCatalogItem, Long> {

    Optional<RewardCatalogItem> findByCode(String code);

    List<RewardCatalogItem> findByKindAndIsActiveTrueOrderByIdAsc(RewardCatalogItem.Kind kind);

    List<RewardCatalogItem> findByKindAndCategoryAndIsActiveTrueOrderByIdAsc(
            RewardCatalogItem.Kind kind,
            RewardCatalogItem.Category category
    );
}
