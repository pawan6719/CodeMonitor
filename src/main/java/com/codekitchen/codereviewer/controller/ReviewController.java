package com.codekitchen.codereviewer.controller;

import com.codekitchen.codereviewer.model.Events;
import com.codekitchen.codereviewer.model.ReviewPayload;
import com.codekitchen.codereviewer.service.ReviewService;

import java.util.Map;

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
            @RequestBody Map<String, Object> reviewPayload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {

                ReviewPayload payload = new ReviewPayload();
                payload.setPayload(reviewPayload);
        if (payload == null || payload.getPullRequest() == null || payload.getRepository() == null) {
            return ResponseEntity.badRequest().body("Expected a valid GitHub pull request webhook payload.");
        }

        if (eventType != null && !Events.PULL_REQUEST.toString().equalsIgnoreCase(eventType)) {
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

    @PostMapping("/push")
    public ResponseEntity<String> reviewPush(
            @RequestBody Map<String, Object> reviewPayload,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType) {

                ReviewPayload payload = new ReviewPayload();
                payload.setPayload(reviewPayload);
        if (payload == null || payload.getRepository() == null) {
            return ResponseEntity.badRequest().body("Expected a valid GitHub push webhook payload.");
        }

        if (eventType != null && !Events.PUSH.toString().equalsIgnoreCase(eventType)) {
            return ResponseEntity.badRequest().body("This endpoint only accepts push webhook events.");
        }

        try {
            String reviewResponse = reviewService.reviewPushRequest(payload);
            return ResponseEntity.ok(reviewResponse);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unable to process the push review: " + ex.getMessage());
        }
    }
}
