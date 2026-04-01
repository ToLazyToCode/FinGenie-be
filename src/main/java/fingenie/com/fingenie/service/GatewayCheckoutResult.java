package fingenie.com.fingenie.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GatewayCheckoutResult {
    private String checkoutUrl;
    private String gatewayOrderRef;
    private String rawPayload;
}
