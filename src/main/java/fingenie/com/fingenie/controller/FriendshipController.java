package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.entity.Friendship;
import fingenie.com.fingenie.dto.FriendshipRequest;
import fingenie.com.fingenie.dto.FriendshipResponse;
import fingenie.com.fingenie.service.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("${api-prefix}/friendships")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping
    public FriendshipResponse create(@Valid @RequestBody FriendshipRequest request) {
        return friendshipService.create(request);
    }

    @GetMapping
    public List<FriendshipResponse> getAll() {
        return friendshipService.getAll();
    }

    @GetMapping("/status/{status}")
    public List<FriendshipResponse> getByStatus(@PathVariable String status) {
        return friendshipService.getByStatus(Friendship.FriendshipStatus.valueOf(status.toUpperCase()));
    }

    @GetMapping("/{friendshipId}")
    public FriendshipResponse getById(@PathVariable Long friendshipId) {
        return friendshipService.getById(friendshipId);
    }

    @PutMapping("/{friendshipId}/status")
    public FriendshipResponse updateStatus(
            @PathVariable Long friendshipId,
            @RequestParam String status
    ) {
        return friendshipService.updateStatus(
                friendshipId,
                Friendship.FriendshipStatus.valueOf(status.toUpperCase())
        );
    }

    @DeleteMapping("/{friendshipId}")
    public ResponseEntity<?> delete(@PathVariable Long friendshipId) {
        friendshipService.delete(friendshipId);
        return ResponseEntity.ok(Map.of("message", "Friendship deleted successfully"));
    }
}
