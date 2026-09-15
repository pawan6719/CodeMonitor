package com.codekitchen.codereviewer.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ModelClassesTest {

    @Test
    @DisplayName("Should verify Severity enum values")
    void testSeverityEnum() {
        assertEquals(Severity.CRITICAL, Severity.valueOf("CRITICAL"));
        assertEquals(Severity.WARNING, Severity.valueOf("WARNING"));
        assertEquals(Severity.SUGGESTION, Severity.valueOf("SUGGESTION"));
        assertEquals(3, Severity.values().length);
    }

    @Test
    @DisplayName("Should verify Events enum values")
    void testEventsEnum() {
        assertEquals(Events.PUSH, Events.valueOf("PUSH"));
        assertEquals(Events.PULL_REQUEST, Events.valueOf("PULL_REQUEST"));
        assertEquals(2, Events.values().length);
    }

    @Test
    @DisplayName("Should verify Metrics getters and setters")
    void testMetrics() {
        Metrics metrics = new Metrics();
        metrics.setLinesOfCode("150");
        metrics.setCodeCoverage("85%");
        metrics.setSecurityScore("9/10");
        metrics.setPerformanceScore("8/10");
        metrics.setReadabilityScore("9/10");

        assertEquals("150", metrics.getLinesOfCode());
        assertEquals("85%", metrics.getCodeCoverage());
        assertEquals("9/10", metrics.getSecurityScore());
        assertEquals("8/10", metrics.getPerformanceScore());
        assertEquals("9/10", metrics.getReadabilityScore());
    }

    @Test
    @DisplayName("Should verify Comment getters, setters and toString")
    void testComment() {
        Comment comment = new Comment();
        comment.setFile("src/Main.java");
        comment.setLine("42");
        comment.setComment("Use try-with-resources");
        comment.setSeverity(Severity.WARNING);
        comment.setSuggestion("Wrap in try(...) block");

        assertEquals("src/Main.java", comment.getFile());
        assertEquals("42", comment.getLine());
        assertEquals("Use try-with-resources", comment.getComment());
        assertEquals(Severity.WARNING, comment.getSeverity());
        assertEquals("Wrap in try(...) block", comment.getSuggestion());
        assertNotNull(comment.toString());
    }

    @Test
    @DisplayName("Should verify PRReviewSchema constructor and getters")
    void testPRReviewSchema() {
        Instant now = Instant.now();
        List<String> strengths = List.of("Clean code", "Good error handling");
        List<String> weaknesses = List.of("Missing tests");

        PRReviewSchema schema = new PRReviewSchema(
                "Good PR",
                "https://github.com/owner/repo/pull/1",
                "200",
                "90%",
                "9/10",
                "8/10",
                "9/10",
                "8.5/10",
                strengths,
                weaknesses,
                now
        );

        assertEquals("Good PR", schema.getReviewSummary());
        assertEquals("https://github.com/owner/repo/pull/1", schema.getPullRequest());
        assertEquals("200", schema.getLinesOfCode());
        assertEquals("90%", schema.getCodeCoverage());
        assertEquals("9/10", schema.getSecurityScore());
        assertEquals("8/10", schema.getPerformanceScore());
        assertEquals("9/10", schema.getReadabilityScore());
        assertEquals("8.5/10", schema.getOverallScore());
        assertEquals(strengths, schema.getStrengths());
        assertEquals(weaknesses, schema.getWeaknesses());
        assertEquals(now, schema.getCreatedAt());

        schema.setReviewSummary("Updated Summary");
        assertEquals("Updated Summary", schema.getReviewSummary());
    }

    @Test
    @DisplayName("Should verify ReviewDocument constructors and getters/setters")
    void testReviewDocument() {
        Instant created = Instant.now();
        Instant updated = Instant.now();
        PRReviewSchema pr = new PRReviewSchema("Summary", "url", "10", "80%", "8", "8", "8", "8", List.of(), List.of(), created);

        ReviewDocument doc = new ReviewDocument("doc-1", "user-123", created, updated, "Progress is good", List.of(pr));
        assertEquals("doc-1", doc.getId());
        assertEquals("user-123", doc.getUserId());
        assertEquals(created, doc.getCreatedAt());
        assertEquals(updated, doc.getUpdatedAt());
        assertEquals("Progress is good", doc.getUserProgressSummary());
        assertEquals(1, doc.getPrReviewSchemas().size());

        ReviewDocument emptyDoc = new ReviewDocument();
        emptyDoc.setId("doc-2");
        emptyDoc.setUserId("user-456");
        emptyDoc.setCreatedAt(created);
        emptyDoc.setUpdatedAt(updated);
        emptyDoc.setUserProgressSummary("Improving");
        emptyDoc.setPrReviewSchemas(List.of());

        assertEquals("doc-2", emptyDoc.getId());
        assertEquals("user-456", emptyDoc.getUserId());
        assertEquals(created, emptyDoc.getCreatedAt());
        assertEquals(updated, emptyDoc.getUpdatedAt());
        assertEquals("Improving", emptyDoc.getUserProgressSummary());
        assertTrue(emptyDoc.getPrReviewSchemas().isEmpty());
    }

    @Test
    @DisplayName("Should verify GenAIReviewSchema getters, setters and toString")
    void testGenAIReviewSchema() {
        GenAIReviewSchema schema = new GenAIReviewSchema();
        schema.setProjectId("project-1");
        schema.setProjectName("CodeReviewer");
        schema.setPullRequestNumber("5");
        schema.setPullRequestTitle("Feature X");
        schema.setPullRequestDescription("Adds feature X");
        schema.setPullRequestAuthor("developer");
        schema.setCommitId("abc1234");
        schema.setReviewer("Gemini-AI");
        schema.setSummary("Overall solid PR");
        Metrics metrics = new Metrics();
        metrics.setSecurityScore("9/10");
        schema.setMetrics(metrics);
        schema.setComments(List.of());
        schema.setOverallScore("9/10");
        schema.setStrengths(List.of("Modular"));
        schema.setWeaknesses(List.of("Needs docs"));
        schema.setUserProgress("Good improvement");

        assertEquals("project-1", schema.getProjectId());
        assertEquals("CodeReviewer", schema.getProjectName());
        assertEquals("5", schema.getPullRequestNumber());
        assertEquals("Feature X", schema.getPullRequestTitle());
        assertEquals("Adds feature X", schema.getPullRequestDescription());
        assertEquals("developer", schema.getPullRequestAuthor());
        assertEquals("abc1234", schema.getCommitId());
        assertEquals("Gemini-AI", schema.getReviewer());
        assertEquals("Overall solid PR", schema.getSummary());
        assertEquals(metrics, schema.getMetrics());
        assertTrue(schema.getComments().isEmpty());
        assertEquals("9/10", schema.getOverallScore());
        assertEquals(List.of("Modular"), schema.getStrengths());
        assertEquals(List.of("Needs docs"), schema.getWeaknesses());
        assertEquals("Good improvement", schema.getUserProgress());
        assertNotNull(schema.toString());
    }
}
