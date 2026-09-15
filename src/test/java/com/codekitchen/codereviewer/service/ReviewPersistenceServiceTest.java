package com.codekitchen.codereviewer.service;

import com.codekitchen.codereviewer.model.*;
import com.codekitchen.codereviewer.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewPersistenceServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new ReviewPersistenceService(reviewRepository);
    }

    @Test
    @DisplayName("Should save new review document when no prior history exists for user")
    void saveReview_shouldCreateNewDocumentForNewUser() {
        ReviewPayload payload = new ReviewPayload(Map.of(
                "pull_request", Map.of(
                        "number", 5,
                        "title", "Feature PR",
                        "html_url", "https://github.com/owner/repo/pull/5",
                        "user", Map.of("login", "alice")
                ),
                "repository", Map.of("full_name", "owner/repo")
        ));

        GenAIReviewSchema review = new GenAIReviewSchema();
        review.setPullRequestTitle("Feature PR");
        review.setPullRequestNumber("5");
        review.setSummary("Well written");
        review.setOverallScore("9/10");
        review.setUserProgress("Good trajectory");
        review.setStrengths(List.of("Clean architecture"));
        review.setWeaknesses(List.of("Add more comments"));
        Metrics metrics = new Metrics();
        metrics.setLinesOfCode("120");
        metrics.setCodeCoverage("90%");
        metrics.setSecurityScore("10/10");
        metrics.setPerformanceScore("8/10");
        metrics.setReadabilityScore("9/10");
        review.setMetrics(metrics);

        when(reviewRepository.findTopByUserIdOrderByCreatedAtDesc("alice")).thenReturn(Optional.empty());
        when(reviewRepository.save(any(ReviewDocument.class))).thenAnswer(invocation -> {
            ReviewDocument doc = invocation.getArgument(0);
            doc.setId("generated-doc-id-1");
            return doc;
        });

        ReviewDocument saved = persistenceService.saveReview(payload, Events.PULL_REQUEST.name(), review);

        assertNotNull(saved);
        assertEquals("generated-doc-id-1", saved.getId());
        assertEquals("alice", saved.getUserId());
        assertEquals("Good trajectory", saved.getUserProgressSummary());
        assertNotNull(saved.getPrReviewSchemas());
        assertEquals(1, saved.getPrReviewSchemas().size());

        PRReviewSchema prReview = saved.getPrReviewSchemas().get(0);
        assertEquals("Well written", prReview.getReviewSummary());
        assertEquals("https://github.com/owner/repo/pull/5", prReview.getPullRequest());
        assertEquals("120", prReview.getLinesOfCode());
        assertEquals("90%", prReview.getCodeCoverage());
        assertEquals("10/10", prReview.getSecurityScore());
        assertEquals("8/10", prReview.getPerformanceScore());
        assertEquals("9/10", prReview.getReadabilityScore());
        assertEquals("9/10", prReview.getOverallScore());

        verify(reviewRepository).save(any(ReviewDocument.class));
    }

    @Test
    @DisplayName("Should append to existing review document when prior history exists for user")
    void saveReview_shouldAppendToExistingDocumentForExistingUser() {
        ReviewPayload payload = new ReviewPayload(Map.of(
                "pull_request", Map.of(
                        "number", 10,
                        "title", "Second PR",
                        "html_url", "https://github.com/owner/repo/pull/10",
                        "user", Map.of("login", "bob")
                ),
                "repository", Map.of("full_name", "owner/repo")
        ));

        GenAIReviewSchema review = new GenAIReviewSchema();
        review.setPullRequestTitle("Second PR");
        review.setPullRequestNumber("10");
        review.setSummary("Second PR Summary");
        review.setOverallScore("8/10");
        review.setUserProgress("Addressed previous comments");
        review.setStrengths(List.of("Tested"));
        review.setWeaknesses(List.of());
        Metrics metrics = new Metrics();
        metrics.setLinesOfCode("50");
        metrics.setCodeCoverage("95%");
        metrics.setSecurityScore("8/10");
        metrics.setPerformanceScore("8/10");
        metrics.setReadabilityScore("8/10");
        review.setMetrics(metrics);

        ReviewDocument existingDoc = new ReviewDocument();
        existingDoc.setId("existing-doc-id");
        existingDoc.setUserId("bob");
        existingDoc.setCreatedAt(Instant.now().minusSeconds(3600));
        List<PRReviewSchema> existingList = new ArrayList<>();
        existingList.add(new PRReviewSchema("First PR Summary", "url-1", "20", "80%", "7", "7", "7", "7", List.of(), List.of(), Instant.now()));
        existingDoc.setPrReviewSchemas(existingList);

        when(reviewRepository.findTopByUserIdOrderByCreatedAtDesc("bob")).thenReturn(Optional.of(existingDoc));
        when(reviewRepository.save(any(ReviewDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDocument saved = persistenceService.saveReview(payload, Events.PULL_REQUEST.name(), review);

        assertEquals("existing-doc-id", saved.getId());
        assertEquals(2, saved.getPrReviewSchemas().size());
        assertEquals("First PR Summary", saved.getPrReviewSchemas().get(0).getReviewSummary());
        assertEquals("Second PR Summary", saved.getPrReviewSchemas().get(1).getReviewSummary());
        assertEquals("Addressed previous comments", saved.getUserProgressSummary());
    }

    @Test
    @DisplayName("Should use sender login when eventType is not PULL_REQUEST")
    void saveReview_shouldUseSenderLoginWhenNotPullRequestEvent() {
        ReviewPayload payload = new ReviewPayload(Map.of(
                "pull_request", Map.of(
                        "number", 2,
                        "title", "Push Review PR",
                        "html_url", "https://github.com/owner/repo/pull/2"
                ),
                "sender", Map.of("login", "pusher-user"),
                "repository", Map.of("full_name", "owner/repo")
        ));

        GenAIReviewSchema review = new GenAIReviewSchema();
        review.setPullRequestTitle("Push Review PR");
        review.setPullRequestNumber("2");
        review.setSummary("Push Review Summary");
        review.setOverallScore("7/10");
        review.setUserProgress("Push progress");
        review.setStrengths(List.of());
        review.setWeaknesses(List.of());
        Metrics metrics = new Metrics();
        review.setMetrics(metrics);

        when(reviewRepository.findTopByUserIdOrderByCreatedAtDesc("pusher-user")).thenReturn(Optional.empty());
        when(reviewRepository.save(any(ReviewDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewDocument saved = persistenceService.saveReview(payload, "PUSH", review);

        assertEquals("pusher-user", saved.getUserId());
        verify(reviewRepository).findTopByUserIdOrderByCreatedAtDesc("pusher-user");
    }

    @Test
    @DisplayName("Should throw RuntimeException when review PR title does not match payload PR title")
    void saveReview_shouldThrowWhenPrTitleDoesNotMatch() {
        ReviewPayload payload = new ReviewPayload(Map.of(
                "pull_request", Map.of(
                        "number", 3,
                        "title", "Expected Title"
                )
        ));

        GenAIReviewSchema review = new GenAIReviewSchema();
        review.setPullRequestTitle("Different Title");
        review.setPullRequestNumber("3");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                persistenceService.saveReview(payload, Events.PULL_REQUEST.name(), review)
        );
        assertEquals("Payload Pull Request does not match GenAI response", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw RuntimeException when review PR number does not match payload PR number")
    void saveReview_shouldThrowWhenPrNumberDoesNotMatch() {
        ReviewPayload payload = new ReviewPayload(Map.of(
                "pull_request", Map.of(
                        "number", 3,
                        "title", "Matching Title"
                )
        ));

        GenAIReviewSchema review = new GenAIReviewSchema();
        review.setPullRequestTitle("Matching Title");
        review.setPullRequestNumber("999");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                persistenceService.saveReview(payload, Events.PULL_REQUEST.name(), review)
        );
        assertEquals("Payload Pull Request does not match GenAI response", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should retrieve review document by ID via getReview")
    void getReview_shouldReturnDocumentById() {
        ReviewDocument mockDoc = new ReviewDocument();
        mockDoc.setId("doc-123");
        when(reviewRepository.findById("doc-123")).thenReturn(Optional.of(mockDoc));

        Optional<ReviewDocument> result = persistenceService.getReview("doc-123");

        assertTrue(result.isPresent());
        assertEquals("doc-123", result.get().getId());
        verify(reviewRepository).findById("doc-123");
    }
}
