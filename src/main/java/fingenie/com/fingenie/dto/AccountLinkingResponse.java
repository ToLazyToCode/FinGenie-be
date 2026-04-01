package fingenie.com.fingenie.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for account linking operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response for account linking and provider management")
public class AccountLinkingResponse {

    @Schema(description = "Response code", example = "SUCCESS")
    private String code;

    @Schema(description = "Human-readable message")
    private String message;

    @Schema(description = "List of linked providers")
    private List<ProviderInfo> providers;

    /**
     * Information about a linked provider
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderInfo {
        @Schema(description = "Provider type", example = "GOOGLE")
        private String provider;

        @Schema(description = "Email associated with provider")
        private String email;

        @Schema(description = "Display name from provider")
        private String displayName;

        @Schema(description = "Profile picture URL")
        private String pictureUrl;

        @Schema(description = "Whether this provider is active")
        private boolean active;

        @Schema(description = "When the provider was linked")
        private String linkedAt;
    }

    // Factory methods
    public static AccountLinkingResponse success(String message, List<ProviderInfo> providers) {
        return AccountLinkingResponse.builder()
                .code("SUCCESS")
                .message(message)
                .providers(providers)
                .build();
    }

    public static AccountLinkingResponse success(String message) {
        return AccountLinkingResponse.builder()
                .code("SUCCESS")
                .message(message)
                .build();
    }
}
