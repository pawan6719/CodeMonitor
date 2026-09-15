package com.codekitchen.codereviewer.controller;

import com.codekitchen.codereviewer.model.Events;
import com.codekitchen.codereviewer.model.ReviewPayload;
import com.codekitchen.codereviewer.service.ReviewService;


import org.springframework.http.HttpStatus;
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

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/pull")
    public ResponseEntity<String> reviewPull(
            @RequestBody ReviewPayload payload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {

        if (payload == null || payload.getPullRequest() == null || payload.getRepository() == null) {
            return ResponseEntity.badRequest().body("Expected a valid GitHub pull request webhook payload.");
        }

        if (eventType != null && !Events.PULL_REQUEST.name().equalsIgnoreCase(eventType)) {
            return ResponseEntity.badRequest().body("This endpoint only accepts pull_request webhook events.");
        }

        if (!payload.getAction().equalsIgnoreCase("opened")){
            return ResponseEntity.ok().body("Review process does not run for " + payload.getAction() + " action on a PR");
        }
        try {
            reviewService.reviewPullRequest(payload);
            return ResponseEntity.ok("Review Process is in progress. Check the Pull request in some time");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to process the pull request review: " + ex.getMessage());
        }
    }
}
