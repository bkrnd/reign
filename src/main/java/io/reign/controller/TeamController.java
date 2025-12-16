package io.reign.controller;

import io.reign.dto.CreateTeamRequest;
import io.reign.dto.JoinTeamRequest;
import io.reign.dto.TeamResponse;
import io.reign.model.Team;
import io.reign.model.User;
import io.reign.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/worlds/{worldSlug}/teams")
public class TeamController {

    @Autowired
    private TeamService teamService;

    /**
     * Get all teams for a world
     */
    @GetMapping
    public ResponseEntity<List<Team>> getTeams(@PathVariable String worldSlug) {
        try {
            List<Team> teams = teamService.getTeamsByWorld(worldSlug);
            return ResponseEntity.ok(teams);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get available teams (not full) for a world
     */
    @GetMapping("/available")
    public ResponseEntity<List<Team>> getAvailableTeams(@PathVariable String worldSlug) {
        try {
            List<Team> teams = teamService.getAvailableTeams(worldSlug);
            return ResponseEntity.ok(teams);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Join an existing team
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinTeam(
            @PathVariable String worldSlug,
            @Valid @RequestBody JoinTeamRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if user is authenticated
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
        }

        try {
            TeamResponse teamResponse = teamService.joinTeam(worldSlug, request.getTeamId(), authenticatedUser.getId());
            return ResponseEntity.ok(teamResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("An error occurred while joining the team"));
        }
    }

    /**
     * Create a new team
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTeam(
            @PathVariable String worldSlug,
            @Valid @RequestBody CreateTeamRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        // Check if user is authenticated
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
        }

        try {
            TeamResponse teamResponse = teamService.createTeam(
                    worldSlug,
                    request.getName(),
                    request.getColor(),
                    authenticatedUser.getId()
            );
            return ResponseEntity.ok(teamResponse);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return ResponseEntity.status(409).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(createErrorResponse("An error occurred while creating the team: " + e.getMessage()));
        }
    }

    /**
     * Check if user is in a team
     */
    @GetMapping("/check-membership")
    public ResponseEntity<Map<String, Object>> checkMembership(
            @PathVariable String worldSlug,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            boolean isMember = teamService.isUserInTeam(worldSlug, authenticatedUser.getId());
            Map<String, Object> response = new HashMap<>();
            response.put("isMember", isMember);

            if (isMember) {
                teamService.getUserTeamResponseInWorld(worldSlug, authenticatedUser.getId())
                        .ifPresent(teamResponse -> response.put("team", teamResponse));
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Leave the current team
     */
    @DeleteMapping("/leave")
    public ResponseEntity<?> leaveTeam(
            @PathVariable String worldSlug,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).body(createErrorResponse("User not authenticated"));
        }

        try {
            teamService.leaveTeam(worldSlug, authenticatedUser.getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Successfully left the team");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(createErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(createErrorResponse("An error occurred while leaving the team"));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
