package fingenie.com.fingenie.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinPointHistoryPageResponse {
    private List<FinPointHistoryItemResponse> items;
    private Integer page;
    private Integer size;
    private Long totalItems;
    private Boolean hasNext;
}
