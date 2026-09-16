package com.codekitchen.codereviewer.component;

import com.codekitchen.codereviewer.component.GithubClient.GitHubFile;
import com.codekitchen.codereviewer.component.GithubClient.GitHubPullRequest;
import com.codekitchen.codereviewer.component.GithubClient.GitHubRepository;
import com.codekitchen.codereviewer.component.GithubClient.GitHubWebhookPayload;
import com.codekitchen.codereviewer.component.GithubClient.GithubLineComment;
import com.codekitchen.codereviewer.component.GithubClient.GithubReviewRequest;
import com.codekitchen.codereviewer.model.Comment;
import com.codekitchen.codereviewer.model.GenAIReviewSchema;
import com.codekitchen.codereviewer.model.Metrics;
import com.codekitchen.codereviewer.model.Severity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GithubClientTest {

    private MockWebServer mockWebServer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should successfully fetch PR files from GitHub API with correct headers and parse response")
    void fetchPullRequestFiles_shouldReturnFiles() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        GithubClient client = new GithubClient(baseUrl, "test-token-123");

        String mockResponseBody = """
                [
                    {
                        "filename": "src/App.java",
                        "status": "modified",
                        "patch": "@@ -1,3 +1,3 @@"
                    },
                    {
                        "filename": "src/Util.java",
                        "status": "added",
                        "patch": "@@ -0,0 +1,10 @@"
                    }
                ]
                """;

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(mockResponseBody)
                .setResponseCode(200));

        List<GitHubFile> files = client.fetchPullRequestFiles("test-owner", "test-repo", 7);

        assertNotNull(files);
        assertEquals(2, files.size());
        assertEquals("src/App.java", files.get(0).filename());
        assertEquals("modified", files.get(0).status());
        assertEquals("@@ -1,3 +1,3 @@", files.get(0).patch());
        assertEquals("src/Util.java", files.get(1).filename());
        assertEquals("added", files.get(1).status());

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/repos/test-owner/test-repo/pulls/7/files", request.getPath());
        assertEquals("GET", request.getMethod());
        assertEquals("Bearer test-token-123", request.getHeader("Authorization"));
        assertEquals("application/vnd.github+json", request.getHeader("Accept"));
        assertEquals("2022-11-28", request.getHeader("X-GitHub-Api-Version"));
        assertEquals("CodeMonitor", request.getHeader("User-Agent"));
    }

    @Test
    @DisplayName("Should successfully post PR review with line comments and formatted overall summary")
    void postPullRequestReview_shouldPostFormattedReview() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        GithubClient client = new GithubClient(baseUrl, "test-token-xyz");

        GenAIReviewSchema schema = new GenAIReviewSchema();
        schema.setPullRequestNumber("15");
        schema.setSummary("PR looks mostly good with minor issues.");
        schema.setOverallScore("8");
        schema.setUserProgress("Good improvement in error handling.");

        Metrics metrics = new Metrics();
        metrics.setSecurityScore("9");
        metrics.setPerformanceScore("8");
        schema.setMetrics(metrics);

        Comment comment = new Comment();
        comment.setFile("src/Service.java");
        comment.setLine("25");
        comment.setSeverity(Severity.WARNING);
        comment.setComment("Resource leak potential");
        comment.setSuggestion("Use try-with-resources");
        schema.setComments(List.of(comment));

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"id\": 12345, \"state\": \"COMMENTED\"}")
                .setResponseCode(200));

        String response = client.postPullRequestReview("owner-abc", "repo-def", "commit-sha-999", schema);

        assertNotNull(response);
        assertTrue(response.contains("12345"));

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("/repos/owner-abc/repo-def/pulls/15/reviews", request.getPath());
        assertEquals("POST", request.getMethod());
        assertEquals("Bearer test-token-xyz", request.getHeader("Authorization"));

        Map<String, Object> requestBody = objectMapper.readValue(request.getBody().readUtf8(), new TypeReference<Map<String, Object>>() {});
        assertEquals("COMMENT", requestBody.get("event"));
        assertEquals("commit-sha-999", requestBody.get("commit_id"));

        String bodyText = (String) requestBody.get("body");
        assertTrue(bodyText.contains("PR looks mostly good with minor issues."));
        assertTrue(bodyText.contains("* **Overall Score:** 8/10"));
        assertTrue(bodyText.contains("* **Security:** 9"));
        assertTrue(bodyText.contains("* **Performance:** 8"));
        assertTrue(bodyText.contains("Good improvement in error handling."));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> comments = (List<Map<String, Object>>) requestBody.get("comments");
        assertEquals(1, comments.size());
        assertEquals("src/Service.java", comments.get(0).get("path"));
        assertEquals(25, comments.get(0).get("line"));
        assertEquals("RIGHT", comments.get(0).get("side"));
        String commentBody = (String) comments.get(0).get("body");
        assertTrue(commentBody.contains("**[WARNING]** Resource leak potential"));
        assertTrue(commentBody.contains("*Suggestion:* Use try-with-resources"));
    }

    @Test
    @DisplayName("Should initialize client without Authorization header when token is null or blank")
    void constructor_shouldOmitAuthorizationHeaderWhenTokenBlank() throws Exception {
        String baseUrl = mockWebServer.url("/").toString();
        GithubClient clientWithoutToken = new GithubClient(baseUrl, "   ");

        mockWebServer.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("[]")
                .setResponseCode(200));

        clientWithoutToken.fetchPullRequestFiles("owner", "repo", 1);

        RecordedRequest request = mockWebServer.takeRequest();
        assertNull(request.getHeader("Authorization"));
    }

    @Test
    @DisplayName("Should verify GithubClient record types")
    void testGithubClientRecords() {
        GithubLineComment lineComment = new GithubLineComment("file.java", 10, "RIGHT", "comment body");
        assertEquals("file.java", lineComment.path());
        assertEquals(10, lineComment.line());
        assertEquals("RIGHT", lineComment.side());
        assertEquals("comment body", lineComment.body());

        GithubReviewRequest reviewRequest = new GithubReviewRequest("summary", "COMMENT", "sha", List.of(lineComment));
        assertEquals("summary", reviewRequest.body());
        assertEquals("COMMENT", reviewRequest.event());
        assertEquals("sha", reviewRequest.commit_id());
        assertEquals(1, reviewRequest.comments().size());

        GitHubPullRequest pr = new GitHubPullRequest(1, "Title", "Body");
        assertEquals(1, pr.number());
        assertEquals("Title", pr.title());
        assertEquals("Body", pr.body());

        GitHubRepository repo = new GitHubRepository("owner/repo");
        assertEquals("owner/repo", repo.full_name());

        GitHubWebhookPayload webhookPayload = new GitHubWebhookPayload("opened", pr, repo);
        assertEquals("opened", webhookPayload.action());
        assertEquals(pr, webhookPayload.pull_request());
        assertEquals(repo, webhookPayload.repository());

        GitHubFile file = new GitHubFile("Main.java", "modified", "+ test");
        assertEquals("Main.java", file.filename());
        assertEquals("modified", file.status());
        assertEquals("+ test", file.patch());
    }
}
