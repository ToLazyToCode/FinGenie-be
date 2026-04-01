package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletRequest {
    
    @NotBlank(message = "Wallet name is required")
    @Size(max = 100, message = "Wallet name must not exceed 100 characters")
    private String walletName;

    private BigDecimal balance;

    private boolean isDefault;
}
