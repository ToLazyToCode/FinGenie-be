package fingenie.com.fingenie.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.sql.Date;

@Data
public class UserProfileRequest {
    
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    private String fullName;
    
    private Date dateOfBirth;
}
