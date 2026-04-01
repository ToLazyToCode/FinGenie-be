package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.utils.SecurityUtils;
import fingenie.com.fingenie.dto.LeaderboardResponse;
import fingenie.com.fingenie.dto.LeaderboardResponse.UserRankInfo;
import fingenie.com.fingenie.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leaderboard")
@Tag(name = "Leaderboard", description = "Leaderboard and rankings APIs")
@SecurityRequirement(name = "bearerAuth")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "Get global leaderboard", description = "Returns the global XP leaderboard with user rankings")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Leaderboard retrieved",
            content = @Content(schema = @Schema(implementation = LeaderboardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<LeaderboardResponse> getGlobalLeaderboard(
            @RequestParam(defaultValue = "100") int limit) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(leaderboardService.getGlobalLeaderboard(accountId, limit));
    }

    @GetMapping("/me")
    @Operation(
        summary = "Get my rank", 
        description = "Returns the current user's rank information including global rank, " +
                     "friends rank, percentile, and points needed to advance."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User rank info returned",
            content = @Content(schema = @Schema(implementation = UserRankInfo.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<UserRankInfo> getMyRank() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(leaderboardService.getMyRank(accountId));
    }

    @GetMapping("/top10")
    @Operation(summary = "Get top 10 players", description = "Returns the top 10 players by XP")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Top 10 returned",
            content = @Content(schema = @Schema(implementation = LeaderboardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<LeaderboardResponse> getTopTen() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(leaderboardService.getTopTen(accountId));
    }

    @GetMapping("/friends")
    @Operation(summary = "Get friends leaderboard", description = "Returns leaderboard of user's friends")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Friends leaderboard returned",
            content = @Content(schema = @Schema(implementation = LeaderboardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<LeaderboardResponse> getFriendsLeaderboard() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(leaderboardService.getFriendsLeaderboard(accountId));
    }

    @GetMapping("/by-level")
    @Operation(summary = "Get leaderboard sorted by level", description = "Returns leaderboard sorted by player level")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Level leaderboard returned",
            content = @Content(schema = @Schema(implementation = LeaderboardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<LeaderboardResponse> getLevelLeaderboard(
            @RequestParam(defaultValue = "100") int limit) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(leaderboardService.getLevelLeaderboard(accountId, limit));
    }
}
