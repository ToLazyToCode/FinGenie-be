package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.dto.UserProfileRequest;
import fingenie.com.fingenie.dto.UserProfileResponse;
import fingenie.com.fingenie.dto.UserSearchResponse;
import fingenie.com.fingenie.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/user-profile")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile management and user search")
@SecurityRequirement(name = "bearerAuth")
public class UserProfileController {

    private final UserProfileService userProfileService;

    @PostMapping
    @Operation(summary = "Create or update user profile", 
               description = "Creates a new profile or updates existing profile for the authenticated user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile saved successfully",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public UserProfileResponse createOrUpdate(@Valid @RequestBody UserProfileRequest request) {
        return userProfileService.createOrUpdate(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile found",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "Profile not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public UserProfileResponse getCurrentProfile() {
        return userProfileService.getCurrentProfile();
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get profile by account ID", description = "Returns a user profile by account ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile found",
            content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "404", description = "Profile not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public UserProfileResponse getByAccountId(
        @Parameter(description = "Account ID", required = true, example = "1")
        @PathVariable Long accountId
    ) {
        return userProfileService.getByAccountId(accountId);
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search users", 
        description = "Search users by email or name for friend discovery. " +
                     "Returns up to 20 results per page. Current user is excluded from results."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = UserSearchResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public List<UserSearchResponse> searchUsers(
        @Parameter(description = "Search query (email or name)", required = true, example = "john")
        @RequestParam String q,
        @Parameter(description = "Page number (0-indexed)", example = "0")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Page size (max 20)", example = "10")
        @RequestParam(defaultValue = "10") int size
    ) {
        // Cap size at 20 to prevent large queries
        int cappedSize = Math.min(size, 20);
        return userProfileService.searchUsers(q, page, cappedSize);
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user profile", description = "Deletes the authenticated user's profile")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> delete() {
        userProfileService.delete();
        return ResponseEntity.ok(Map.of("message", "Profile deleted successfully"));
    }
}
