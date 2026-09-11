package com.codekitchen.codereviewer.controller;

import com.codekitchen.codereviewer.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /****
     * This is a comment to test webhook functionality once more. Addendum Comment
     */
    @PostMapping("/webhooks/github")
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestBody ReviewService.GitHubWebhookPayload payload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {

        if (payload == null || payload.pull_request() == null || payload.repository() == null) {
            return ResponseEntity.badRequest().body("Expected a valid GitHub pull request webhook payload.");
        }

        if (eventType != null && !"pull_request".equalsIgnoreCase(eventType)) {
            return ResponseEntity.badRequest().body("This endpoint only accepts pull_request webhook events.");
        }

        try {
            String reviewResponse = reviewService.reviewPullRequest(payload);
            return ResponseEntity.ok(reviewResponse);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to process the pull request review: " + ex.getMessage());
        }
    }
}


