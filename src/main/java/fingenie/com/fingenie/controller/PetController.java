package fingenie.com.fingenie.controller;

import fingenie.com.fingenie.ai.client.AIClient;
import fingenie.com.fingenie.ai.client.dto.InsightRequest;
import fingenie.com.fingenie.dto.PetChatRequest;
import fingenie.com.fingenie.dto.PetChatResponse;
import fingenie.com.fingenie.dto.PetProfileResponse;
import fingenie.com.fingenie.dto.PetRequest;
import fingenie.com.fingenie.dto.PetResponse;
import fingenie.com.fingenie.entitlement.EntitlementService;
import fingenie.com.fingenie.service.PetService;
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

import fingenie.com.fingenie.utils.SecurityUtils;

/**
 * Controller for virtual pet interactions.
 * OSIV-SAFE: All endpoints return DTOs, not entities.
 */
@RestController
@RequestMapping("${api-prefix}/pet")
@RequiredArgsConstructor
@Tag(name = "Virtual Pet", description = "AI-powered virtual pet that reacts to financial behavior")
@SecurityRequirement(name = "bearerAuth")
public class PetController {

    private final AIClient aiClient;
    private final PetService petService;
    private final EntitlementService entitlementService;

    // ========== PetProfile State Management ==========

    @GetMapping("/state")
    @Operation(
        summary = "Get pet state",
        description = "Returns the current state of the user's virtual pet including mood, energy, hunger, and happiness."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet state returned",
            content = @Content(schema = @Schema(implementation = PetProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PetProfileResponse> getState() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(petService.getState(accountId));
    }

    @PostMapping("/feed")
    @Operation(
        summary = "Feed the pet",
        description = "Feeds the virtual pet, reducing hunger and increasing happiness."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet fed successfully",
            content = @Content(schema = @Schema(implementation = PetProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PetProfileResponse> feed() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(petService.feed(accountId));
    }

    @PostMapping("/play")
    @Operation(
        summary = "Play with the pet",
        description = "Plays with the virtual pet, increasing energy and happiness."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Played with pet successfully",
            content = @Content(schema = @Schema(implementation = PetProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PetProfileResponse> play() {
        Long accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(petService.play(accountId));
    }

    @PostMapping("/chat")
    @Operation(
        summary = "Chat with the pet",
        description = "Sends a message to the AI-powered virtual pet. The pet responds based on its personality, " +
                     "current mood, and the user's financial context. Chatting increases pet happiness."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet response generated",
            content = @Content(schema = @Schema(implementation = PetChatResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PetChatResponse> chat(@Valid @RequestBody PetChatRequest request) {
        Long accountId = SecurityUtils.getCurrentAccountId();
        entitlementService.assertFeature(accountId, "ai.chat");
        entitlementService.assertAiChatQuota(accountId);
        InsightRequest payload = InsightRequest.builder()
                .accountId(accountId)
                .insightType("PET_CHAT")
                .message(request.getMessage())
                .build();

        Map<String, Object> aiResponse = aiClient.insight(payload);
        PetChatResponse response = PetChatResponse.builder()
                .petMessage(readString(aiResponse, "petMessage", "message", "response"))
                .mood(readString(aiResponse, "mood"))
                .happiness(readInt(aiResponse, "happiness", 50))
                .confidence(readDouble(aiResponse, "confidence", 0.8d))
                .personality(readString(aiResponse, "personality"))
                .build();
        return ResponseEntity.ok(response);
    }

    // ========== Pet CRUD Operations ==========

    @PostMapping
    @Operation(
        summary = "Create a pet",
        description = "Creates a new virtual pet for the user with specified name, type, and personality."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet created successfully",
            content = @Content(schema = @Schema(implementation = PetResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PetResponse> create(@RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.create(request));
    }

    @GetMapping
    @Operation(summary = "Get all pets", description = "Returns all pets for the user.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of pets returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = PetResponse.class)))),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<List<PetResponse>> getAll() {
        return ResponseEntity.ok(petService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pet by ID", description = "Returns a specific pet by its ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet found",
            content = @Content(schema = @Schema(implementation = PetResponse.class))),
        @ApiResponse(responseCode = "404", description = "Pet not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<PetResponse> getById(
        @Parameter(description = "Pet ID", required = true, example = "1")
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(petService.getById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete pet", description = "Deletes a pet by its ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pet deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Pet not found"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<String> delete(
        @Parameter(description = "Pet ID", required = true, example = "1")
        @PathVariable Long id
    ) {
        petService.delete(id);
        return ResponseEntity.ok("Deleted");
    }

    private String readString(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return "";
    }

    private int readInt(Map<String, Object> payload, String key, int defaultValue) {
        Object value = payload.get(key);
        if (value instanceof Number n) {
            return n.intValue();
        }
        return defaultValue;
    }

    private double readDouble(Map<String, Object> payload, String key, double defaultValue) {
        Object value = payload.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return defaultValue;
    }
}
