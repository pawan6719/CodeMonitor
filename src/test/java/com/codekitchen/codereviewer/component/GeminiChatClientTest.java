package com.codekitchen.codereviewer.component;

import com.codekitchen.codereviewer.component.GithubClient.GitHubFile;
import com.codekitchen.codereviewer.model.GenAIReviewSchema;
import com.codekitchen.codereviewer.model.PRReviewSchema;
import com.codekitchen.codereviewer.model.ReviewDocument;
import com.codekitchen.codereviewer.model.ReviewPayload;
import com.codekitchen.codereviewer.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeminiChatClientTest {

    @Mock
    private GoogleGenAiChatModel googleGenAiChatModel;

    @Mock
    private ReviewRepository reviewRepository;

    private GeminiChatClient geminiChatClient;

    @BeforeEach
    void setUp() {
        geminiChatClient = new GeminiChatClient(googleGenAiChatModel, reviewRepository);
    }

    @Test
    @DisplayName("Should build prompt with historical context, call model, and parse GenAIReviewSchema correctly")
    void reviewPullRequest_withHistoricalContext() throws Exception {
        ReviewPayload payload = new ReviewPayload(Map.of(
                "pull_request", Map.of(
                        "number", 4,
                        "title", "Add payment service",
                        "body", "Implements Stripe payment integration",
                        "user", Map.of("login", "dev_user")
                ),
                "repository", Map.of("full_name", "org/repo")
        ));

        List<GitHubFile> files = List.of(
                new GitHubFile("PaymentService.java", "modified", "+ public void pay() {}"),
                new GitHubFile("EmptyPatch.java", "modified", "") // empty patch should be skipped
        );

        ReviewDocument historicalDoc = new ReviewDocument();
        historicalDoc.setUserProgressSummary("Improving on code quality");
        PRReviewSchema pastPr = new PRReviewSchema(
                "Past PR summary",
                "https://github.com/org/repo/pull/1",
                "50", "80%", "7", "7", "7", "7",
                List.of("Solid tests"),
                List.of("Missing null checks"),
                Instant.now()
        );
        historicalDoc.setPrReviewSchemas(List.of(pastPr));

        when(reviewRepository.findByUserId("dev_user"))
                .thenReturn(Optional.of(historicalDoc));

        String rawJsonResponse = """
                {
                    "projectId": "org/repo",
                    "projectName": "repo",
                    "pullRequestNumber": "4",
                    "pullRequestTitle": "Add payment service",
                    "pullRequestDescription": "Implements Stripe payment integration",
                    "pullRequestAuthor": "dev_user",
                    "commitId": "sha123",
                    "reviewer": "Gemini",
                    "summary": "Looks good",
                    "metrics": {
                        "linesOfCode": 10,
                        "codeCoverage": "90%",
                        "securityScore": "8/10",
                        "performanceScore": "8/10",
                        "readabilityScore": "9/10"
                    },
                    "comments": [],
                    "overallScore": "8.5",
                    "strengths": ["Clean structure"],
                    "weaknesses": ["Add integration tests"],
                    "userProgress": "Addressed past null check issues well"
                }
                """;

        AssistantMessage assistantMessage = new AssistantMessage(rawJsonResponse);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        when(googleGenAiChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        GenAIReviewSchema result = geminiChatClient.reviewPullRequest(payload, files);

        assertNotNull(result);
        assertEquals("4", result.getPullRequestNumber());
        assertEquals("Add payment service", result.getPullRequestTitle());
        assertEquals("Looks good", result.getSummary());
        assertEquals("8.5", result.getOverallScore());
        assertEquals("Addressed past null check issues well", result.getUserProgress());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(googleGenAiChatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        String promptContent = prompt.getContents();

        assertTrue(promptContent.contains("Improving on code quality"));
        assertTrue(promptContent.contains("Past Strengths: Solid tests"));
        assertTrue(promptContent.contains("Past Weaknesses: Missing null checks"));
        assertTrue(promptContent.contains("Repository: org/repo"));
        assertTrue(promptContent.contains("Pull request number: #4"));
        assertTrue(promptContent.contains("PaymentService.java"));
        assertFalse(promptContent.contains("EmptyPatch.java"));
    }

    @Test
    @DisplayName("Should build prompt when no historical reviews exist for user")
    void reviewPullRequest_withoutHistoricalContext() throws Exception {
        ReviewPayload payload = new ReviewPayload(Map.of(
                "pull_request", Map.of(
                        "number", 1,
                        "title", "First PR",
                        "user", Map.of("login", "new_dev")
                ),
                "repository", Map.of("full_name", "org/first-repo")
        ));

        List<GitHubFile> files = List.of(
                new GitHubFile("First.java", null, "+ public class First {}")
        );

        when(reviewRepository.findByUserId("new_dev"))
                .thenReturn(Optional.empty());

        String rawJsonResponse = """
                {
                    "projectId": "org/first-repo",
                    "projectName": "first-repo",
                    "pullRequestNumber": "1",
                    "pullRequestTitle": "First PR",
                    "summary": "First review",
                    "overallScore": "9"
                }
                """;

        AssistantMessage assistantMessage = new AssistantMessage(rawJsonResponse);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));

        when(googleGenAiChatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        GenAIReviewSchema result = geminiChatClient.reviewPullRequest(payload, files);

        assertNotNull(result);
        assertEquals("1", result.getPullRequestNumber());
        assertEquals("First PR", result.getPullRequestTitle());

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(googleGenAiChatModel).call(promptCaptor.capture());
        Prompt prompt = promptCaptor.getValue();
        String promptContent = prompt.getContents();

        assertTrue(promptContent.contains("No prior PR Reviews are available for this user"));
        assertTrue(promptContent.contains("STATUS: updated"));
    }
}
