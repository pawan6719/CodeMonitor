package com.codekitchen.codereviewer.controller;

import com.codekitchen.codereviewer.model.ReviewPayload;
import com.codekitchen.codereviewer.service.ReviewService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should return 202 Accepted when valid pull request opened webhook is received with X-GitHub-Event header")
    void reviewPull_shouldReturnOkWithValidPullRequestPayload() throws Exception {
        String payload = readResource("pull_request_valid.json");

        when(reviewService.reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Review process started in background."));

        verify(reviewService, times(1)).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    @Test
    @DisplayName("Should return 202 Accepted when X-GitHub-Event header is omitted but payload is valid and action is opened")
    void reviewPull_shouldReturnOkWhenEventHeaderIsOmitted() throws Exception {
        String payload = readResource("pull_request_valid.json");

        when(reviewService.reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Review process started in background."));

        verify(reviewService, times(1)).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when X-GitHub-Event is not pull_request")
    void reviewPull_shouldReturnBadRequestWhenEventHeaderIsInvalid() throws Exception {
        String payload = readResource("pull_request_valid.json");

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "push")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("This endpoint only accepts pull_request webhook events."));

        verify(reviewService, never()).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    @Test
    @DisplayName("Should return 200 OK with bypass message when PR action is not 'opened'")
    void reviewPull_shouldReturnOkWhenActionIsNotOpened() throws Exception {
        Map<String, Object> map = objectMapper.readValue(readResource("pull_request_valid.json"), new TypeReference<Map<String, Object>>() {});
        map.put("action", "synchronize");
        String payload = objectMapper.writeValueAsString(map);

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("Review process does not run for synchronize action on a PR"));

        verify(reviewService, never()).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when pull_request object is missing in payload")
    void reviewPull_shouldReturnBadRequestWhenPullRequestMissing() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("action", "opened");
        map.put("repository", Map.of("full_name", "owner/repo"));
        String payload = objectMapper.writeValueAsString(map);

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Expected a valid GitHub pull request webhook payload."));

        verify(reviewService, never()).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when repository object is missing in payload")
    void reviewPull_shouldReturnBadRequestWhenRepositoryMissing() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("action", "opened");
        map.put("pull_request", Map.of("number", 1));
        String payload = objectMapper.writeValueAsString(map);

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Expected a valid GitHub pull request webhook payload."));

        verify(reviewService, never()).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when repository full_name does not contain slash")
    void reviewPull_shouldReturnBadRequestWhenRepositoryFormatIsInvalid() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("action", "opened");
        map.put("pull_request", Map.of("number", 1));
        map.put("repository", Map.of("full_name", "invalid-repo"));
        String payload = objectMapper.writeValueAsString(map);

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Repository full name must be in owner/repo format."));

        verify(reviewService, never()).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when pull request number is missing from payload")
    void reviewPull_shouldReturnBadRequestWhenPullRequestNumberMissing() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("action", "opened");
        map.put("pull_request", Map.of("title", "No Number PR"));
        map.put("repository", Map.of("full_name", "owner/repo"));
        String payload = objectMapper.writeValueAsString(map);

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Pull request number is missing from the webhook payload."));

        verify(reviewService, never()).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    @Test
    @DisplayName("Should return 202 Accepted even if background async review completes exceptionally")
    void reviewPull_shouldHandleBackgroundExceptionGracefully() throws Exception {
        String payload = readResource("pull_request_valid.json");

        when(reviewService.reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("GitHub API error")));

        mockMvc.perform(post("/api/review/pull")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-GitHub-Event", "pull_request")
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(content().string("Review process started in background."));

        verify(reviewService, times(1)).reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class));
    }

    private String readResource(String resourceName) throws Exception {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        Path path = Paths.get(resourceUrl.toURI());
        return Files.readString(path);
    }
}


