package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {
    
    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String categoryName;
    
    @NotNull(message = "Category type is required")
    private Category.CategoryType categoryType;
    
    private boolean isSystem;
}
