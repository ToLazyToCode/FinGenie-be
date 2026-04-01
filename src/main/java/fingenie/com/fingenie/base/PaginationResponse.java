package fingenie.com.fingenie.base;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PaginationResponse<T> {
    private T data;
    private int page;
    private int size;
    private long totalPages;
    private long totalElements;
}

