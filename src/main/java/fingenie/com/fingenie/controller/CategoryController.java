package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.entity.Category;
import fingenie.com.fingenie.dto.CategoryRequest;
import fingenie.com.fingenie.dto.CategoryResponse;
import fingenie.com.fingenie.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return categoryService.create(request);
    }

    @GetMapping
    public List<CategoryResponse> getAll() {
        return categoryService.getAll();
    }

    @GetMapping("/type/{type}")
    public List<CategoryResponse> getByType(@PathVariable String type) {
        return categoryService.getByType(Category.CategoryType.valueOf(type.toUpperCase()));
    }

    @GetMapping("/system")
    public List<CategoryResponse> getSystemCategories() {
        return categoryService.getSystemCategories();
    }

    @GetMapping("/{categoryId}")
    public CategoryResponse getById(@PathVariable Long categoryId) {
        return categoryService.getById(categoryId);
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse update(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryRequest request
    ) {
        return categoryService.update(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<?> delete(@PathVariable Long categoryId) {
        categoryService.delete(categoryId);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }
}
