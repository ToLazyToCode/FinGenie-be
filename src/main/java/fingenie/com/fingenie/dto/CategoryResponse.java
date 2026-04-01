package fingenie.com.fingenie.dto;

import fingenie.com.fingenie.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long categoryId;
    private String categoryName;
    private Category.CategoryType categoryType;
    private boolean isSystem;
}
