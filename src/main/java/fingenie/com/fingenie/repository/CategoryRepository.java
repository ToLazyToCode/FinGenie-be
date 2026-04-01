package fingenie.com.fingenie.repository;

import fingenie.com.fingenie.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByCategoryType(Category.CategoryType categoryType);
    List<Category> findByIsSystem(boolean isSystem);
    List<Category> findByCategoryTypeAndIsSystem(Category.CategoryType categoryType, boolean isSystem);
    Optional<Category> findByCategoryName(String categoryName);
}
