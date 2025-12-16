package io.reign.controller;

import io.reign.model.Square;
import io.reign.model.User;
import io.reign.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/worlds/{worldSlug}/actions")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/capture")
    public ResponseEntity<Square> captureSquare(
            @PathVariable String worldSlug,
            @RequestBody CaptureRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        try {
            Square square = gameService.captureSquare(worldSlug, request.getX(), request.getY(), authenticatedUser.getId());
            return ResponseEntity.ok(square);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/defend")
    public ResponseEntity<Square> defendSquare(
            @PathVariable String worldSlug,
            @RequestBody DefendRequest request,
            @AuthenticationPrincipal User authenticatedUser
    ) {
        try {
            Square square = gameService.defendSquare(worldSlug, request.getX(), request.getY(), authenticatedUser.getId());
            return ResponseEntity.ok(square);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

class CaptureRequest {
    private int x;
    private int y;

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}

class DefendRequest {
    private int x;
    private int y;

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}