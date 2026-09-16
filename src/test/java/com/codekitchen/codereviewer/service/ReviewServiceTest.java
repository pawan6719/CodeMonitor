package com.codekitchen.codereviewer.service;

import com.codekitchen.codereviewer.component.GeminiChatClient;
import com.codekitchen.codereviewer.component.GithubClient;
import com.codekitchen.codereviewer.component.GithubClient.GitHubFile;
import com.codekitchen.codereviewer.model.GenAIReviewSchema;
import com.codekitchen.codereviewer.model.Metrics;
import com.codekitchen.codereviewer.model.ReviewPayload;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private ReviewService reviewService;

    @Mock
    private GithubClient githubClient;

    @Mock
    private GeminiChatClient geminiChatClient;

    @Mock
    private ReviewPersistenceService reviewPersistenceService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                geminiChatClient, githubClient, reviewPersistenceService
        );
    }

    @Test
    @DisplayName("Should successfully execute full PR review workflow")
    void reviewPullRequest_shouldFetchFilesPostReviewAndSave() throws Exception {
        ReviewPayload payload = readPayload("pull_request_valid.json");

        List<GitHubFile> mockFiles = List.of(
                new GitHubFile("src/Main.java", "modified", "@@ -1,3 +1,3 @@")
        );

        GenAIReviewSchema mockReview = new GenAIReviewSchema();
        mockReview.setProjectId("codereviewer");
        mockReview.setPullRequestNumber("3");
        mockReview.setOverallScore("9/10");
        mockReview.setSummary("Great PR");
        Metrics metrics = new Metrics();
        mockReview.setMetrics(metrics);

        when(githubClient.fetchPullRequestFiles("pawan6719", "CodeMonitor", 3))
                .thenReturn(mockFiles);
        when(geminiChatClient.reviewPullRequest(eq(payload), eq(mockFiles)))
                .thenReturn(mockReview);
        when(githubClient.postPullRequestReview(eq("pawan6719"), eq("CodeMonitor"), eq("29fd92e99337815f4563e85c4356bc3baecbe6df"), eq(mockReview)))
                .thenReturn("SUCCESS");

        reviewService.reviewPullRequest(payload).join();

        verify(githubClient, times(1)).fetchPullRequestFiles("pawan6719", "CodeMonitor", 3);
        verify(geminiChatClient, times(1)).reviewPullRequest(payload, mockFiles);
        verify(githubClient, times(1)).postPullRequestReview("pawan6719", "CodeMonitor", "29fd92e99337815f4563e85c4356bc3baecbe6df", mockReview);
        verify(reviewPersistenceService, times(1)).saveReview(payload, "pull_request", mockReview);
    }

    @Test
    @DisplayName("Should return failed future with IllegalArgumentException when payload is null")
    void reviewPullRequest_shouldThrowWhenPayloadIsNull() {
        var future = reviewService.reviewPullRequest(null);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Should return failed future with IllegalArgumentException when repository details are missing in payload")
    void reviewPullRequest_shouldThrowWhenRepositoryIsMissing() {
        Map<String, Object> map = new HashMap<>();
        map.put("pull_request", Map.of("number", 1));
        ReviewPayload payload = new ReviewPayload(map);

        var future = reviewService.reviewPullRequest(payload);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Should return failed future with IllegalArgumentException when repository full_name is blank")
    void reviewPullRequest_shouldThrowWhenRepositoryFullNameIsBlank() {
        Map<String, Object> map = new HashMap<>();
        map.put("pull_request", Map.of("number", 1));
        map.put("repository", Map.of("full_name", "   "));
        ReviewPayload payload = new ReviewPayload(map);

        var future = reviewService.reviewPullRequest(payload);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Should return failed future with IllegalArgumentException when pull request number is missing")
    void reviewPullRequest_shouldThrowWhenPullRequestNumberIsMissing() {
        Map<String, Object> map = new HashMap<>();
        map.put("pull_request", Map.of("title", "No Number PR"));
        map.put("repository", Map.of("full_name", "owner/repo"));
        ReviewPayload payload = new ReviewPayload(map);

        var future = reviewService.reviewPullRequest(payload);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Should return failed future with IllegalArgumentException when repository full_name is not owner/repo format")
    void reviewPullRequest_shouldThrowWhenRepositoryFullNameFormatIsInvalid() {
        Map<String, Object> map = new HashMap<>();
        map.put("pull_request", Map.of("number", 1));
        map.put("repository", Map.of("full_name", "invalid-repo-name-without-slash"));
        ReviewPayload payload = new ReviewPayload(map);

        var future = reviewService.reviewPullRequest(payload);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Should return failed future with RuntimeException when GitHubClient returns no changed files")
    void reviewPullRequest_shouldThrowWhenNoChangedFilesFound() {
        Map<String, Object> map = new HashMap<>();
        map.put("pull_request", Map.of("number", 42));
        map.put("repository", Map.of("full_name", "owner/repo"));
        ReviewPayload payload = new ReviewPayload(map);

        when(githubClient.fetchPullRequestFiles("owner", "repo", 42))
                .thenReturn(Collections.emptyList());

        var future = reviewService.reviewPullRequest(payload);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    @DisplayName("Should work without error when reviewPersistenceService is null")
    void reviewPullRequest_shouldWorkWhenPersistenceServiceIsNull() throws Exception {
        ReviewService serviceWithoutPersistence = new ReviewService(geminiChatClient, githubClient, null);
        ReviewPayload payload = readPayload("pull_request_valid.json");

        List<GitHubFile> mockFiles = List.of(new GitHubFile("File.java", "added", "+ code"));
        GenAIReviewSchema mockReview = new GenAIReviewSchema();
        mockReview.setProjectId("test");
        mockReview.setPullRequestNumber("3");
        mockReview.setOverallScore("10");

        when(githubClient.fetchPullRequestFiles("pawan6719", "CodeMonitor", 3)).thenReturn(mockFiles);
        when(geminiChatClient.reviewPullRequest(any(), any())).thenReturn(mockReview);

        serviceWithoutPersistence.reviewPullRequest(payload).join();

        verify(githubClient, times(1)).postPullRequestReview(eq("pawan6719"), eq("CodeMonitor"), eq("29fd92e99337815f4563e85c4356bc3baecbe6df"), eq(mockReview));
    }

    private ReviewPayload readPayload(String resourceName) throws Exception {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        Path path = Paths.get(resourceUrl.toURI());
        String payload = Files.readString(path);
        Map<String, Object> mapPayload = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
        return new ReviewPayload(mapPayload);
    }
}

