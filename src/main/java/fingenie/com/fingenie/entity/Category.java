package fingenie.com.fingenie.entity;

import fingenie.com.fingenie.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Column(name = "category_name", length = 100, nullable = false)
    private String categoryName;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", length = 20, nullable = false)
    private CategoryType categoryType;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private boolean isSystem = false;

    public enum CategoryType {
        INCOME("income"),
        EXPENSE("expense"),
        SAVING("saving");

        private final String value;

        CategoryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
