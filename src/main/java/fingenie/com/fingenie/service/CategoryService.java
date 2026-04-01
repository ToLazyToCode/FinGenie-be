package fingenie.com.fingenie.service;

import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.dto.CategoryRequest;
import fingenie.com.fingenie.dto.CategoryResponse;
import fingenie.com.fingenie.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .categoryType(request.getCategoryType())
                .isSystem(request.isSystem())
                .build();

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    public CategoryResponse getById(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        
        return mapToResponse(category);
    }

    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getByType(Category.CategoryType type) {
        return categoryRepository.findByCategoryType(type).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getSystemCategories() {
        return categoryRepository.findByIsSystem(true).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse update(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Prevent updating system categories
        if (category.isSystem()) {
            throw new RuntimeException("Cannot update system category");
        }

        category.setCategoryName(request.getCategoryName());
        category.setCategoryType(request.getCategoryType());

        category = categoryRepository.save(category);
        return mapToResponse(category);
    }

    @Transactional
    public void delete(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        // Prevent deleting system categories
        if (category.isSystem()) {
            throw new RuntimeException("Cannot delete system category");
        }

        categoryRepository.delete(category);
    }

    /**
     * Seed default system categories if they don't exist
     */
    @Transactional
    public void seedDefaultCategories() {
        if (categoryRepository.count() > 0) {
            return; // Already have categories
        }

        // Expense categories
        String[] expenseCategories = {
            "Food & Dining", "Transportation", "Shopping", "Entertainment",
            "Bills & Utilities", "Healthcare", "Education", "Travel",
            "Personal Care", "Gifts & Donations", "Investment", "Other Expense"
        };

        for (String name : expenseCategories) {
            Category category = Category.builder()
                    .categoryName(name)
                    .categoryType(Category.CategoryType.EXPENSE)
                    .isSystem(true)
                    .build();
            categoryRepository.save(category);
        }

        // Income categories
        String[] incomeCategories = {
            "Salary", "Freelance", "Business", "Investment Income",
            "Rental Income", "Bonus", "Gift", "Refund", "Other Income"
        };

        for (String name : incomeCategories) {
            Category category = Category.builder()
                    .categoryName(name)
                    .categoryType(Category.CategoryType.INCOME)
                    .isSystem(true)
                    .build();
            categoryRepository.save(category);
        }
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getCategoryName())
                .categoryType(category.getCategoryType())
                .isSystem(category.isSystem())
                .build();
    }
}
