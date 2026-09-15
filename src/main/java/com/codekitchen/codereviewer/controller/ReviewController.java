package com.codekitchen.codereviewer.controller;

import com.codekitchen.codereviewer.model.Events;
import com.codekitchen.codereviewer.model.ReviewPayload;
import com.codekitchen.codereviewer.service.ReviewService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewController {

    private final ReviewService reviewService;
    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/pull")
    public ResponseEntity<String> reviewPull(
            @RequestBody ReviewPayload payload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {

        if (payload == null || payload.getPullRequest() == null || payload.getPullRequest().isEmpty()
                || payload.getRepository() == null || payload.getRepository().isEmpty()) {
            return ResponseEntity.badRequest().body("Expected a valid GitHub pull request webhook payload.");
        }

        if (eventType != null && !Events.PULL_REQUEST.name().equalsIgnoreCase(eventType)) {
            return ResponseEntity.badRequest().body("This endpoint only accepts pull_request webhook events.");
        }

        if (payload.getAction() != null && !payload.getAction().equalsIgnoreCase("opened")) {
            return ResponseEntity.ok()
                    .body("Review process does not run for " + payload.getAction() + " action on a PR");
        }
        if (payload.getRepositoryFullName() == null || !payload.getRepositoryFullName().contains("/")) {
            return ResponseEntity.badRequest().body("Repository full name must be in owner/repo format.");
        }

        if (payload.getPullRequestNumber() == null) {
            return ResponseEntity.badRequest().body("Pull request number is missing from the webhook payload.");
        }

        // 2. Dispatch async work with non-blocking error handler
        reviewService.reviewPullRequest(payload)
                .exceptionally(ex -> {
                    log.error("Async review failed for PR #{}: {}", payload.getPullRequestNumber(), ex.getMessage());
                    return null;
                });

        return ResponseEntity.accepted().body("Review process started in background.");
    }
}
