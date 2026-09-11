package com.codekitchen.codereviewer.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final RestClient githubRestClient;
    private final RestClient geminiRestClient;
    private final String geminiApiKey;
    private final String geminiModel;

    public ReviewService(
            @Value("${github.api.url:https://api.github.com}") String githubApiUrl,
            @Value("${github.token:}") String githubToken,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com}") String geminiApiUrl,
            @Value("${gemini.api.key:}") String geminiApiKey,
            @Value("${gemini.model:gemini-2.0-flash}") String geminiModel) {

        this.geminiApiKey = geminiApiKey;
        this.geminiModel = geminiModel;

        RestClient.Builder githubBuilder = RestClient.builder()
                .baseUrl(githubApiUrl)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (githubToken != null && !githubToken.isBlank()) {
            githubBuilder = githubBuilder.defaultHeader("Authorization", "Bearer " + githubToken);
        }

        this.githubRestClient = githubBuilder.build();
        this.geminiRestClient = RestClient.builder().baseUrl(geminiApiUrl).build();
    }

    public String reviewPullRequest(GitHubWebhookPayload payload) {
        if (payload == null || payload.pull_request() == null) {
            throw new IllegalArgumentException("Pull request payload is missing.");
        }

        if (payload.repository() == null || payload.repository().full_name() == null || payload.repository().full_name().isBlank()) {
            throw new IllegalArgumentException("Repository details are missing from the webhook payload.");
        }

        GitHubPullRequest pullRequest = payload.pull_request();

        if (pullRequest.number() == null) {
            throw new IllegalArgumentException("Pull request number is missing from the webhook payload.");
        }

        String[] repositoryParts = payload.repository().full_name().split("/", 2);

        if (repositoryParts.length != 2) {
            throw new IllegalArgumentException("Repository full name must be in owner/repo format.");
        }

        String owner = repositoryParts[0];
        String repo = repositoryParts[1];

        List<GitHubFile> files = fetchPullRequestFiles(owner, repo, pullRequest.number());

        if (files.isEmpty()) {
            return "No changed files were found for pull request #" + pullRequest.number() + ".";
        }

        String prompt = buildReviewPrompt(payload, files);
        String reviewSummary = callGemini(prompt);

        postPullRequestReview(owner, repo, pullRequest.number(), reviewSummary);

        return reviewSummary;
    }

    private List<GitHubFile> fetchPullRequestFiles(String owner, String repo, int pullRequestNumber) {
        List<GitHubFile> files = githubRestClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}/files", owner, repo, pullRequestNumber)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        return files == null ? new ArrayList<>() : files;
    }

    private String buildReviewPrompt(GitHubWebhookPayload payload, List<GitHubFile> files) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a senior code reviewer. Review the pull request changes below for correctness, quality, security, and maintainability.\n");
        prompt.append("Repository: ").append(payload.repository().full_name()).append("\n");
        prompt.append("Pull request number: #").append(payload.pull_request().number()).append("\n");
        prompt.append("Title: ").append(payload.pull_request().title() == null ? "" : payload.pull_request().title()).append("\n");

        if (payload.pull_request().body() != null && !payload.pull_request().body().isBlank()) {
            prompt.append("Description: ").append(payload.pull_request().body()).append("\n");
        }

        prompt.append("\nReturn concise, actionable feedback in markdown. Focus on likely bugs, edge cases, security concerns, code quality, and maintainability.\n\n");

        for (GitHubFile file : files) {
            if (file.patch() == null || file.patch().isBlank()) {
                continue;
            }

            prompt.append("FILE: ").append(file.filename()).append("\n");
            prompt.append("STATUS: ").append(file.status() == null ? "updated" : file.status()).append("\n");
            prompt.append("```diff\n").append(file.patch()).append("\n```\n\n");
        }

        return prompt.toString();
    }

    private String callGemini(String prompt) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is missing. Set gemini.api.key in your configuration.");
        }

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        Map<String, Object> response = geminiRestClient.post()
                .uri("/v1beta/models/{model}:generateContent?key={apiKey}", geminiModel, geminiApiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null || response.get("candidates") == null) {
            throw new IllegalStateException("Gemini AI did not return a review. Check the configured API key and model.");
        }

        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            Map<String, Object> firstCandidate = candidates.getFirst();
            Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            return String.valueOf(parts.getFirst().get("text"));
        } catch (Exception ex) {
            log.error("Unable to parse Gemini response: {}", response, ex);
            throw new IllegalStateException("Gemini AI returned an unexpected payload. Please review the API response format.", ex);
        }
    }

    private void postPullRequestReview(String owner, String repo, int pullRequestNumber, String reviewSummary) {
        Map<String, Object> reviewRequest = Map.of(
                "event", "COMMENT",
                "body", reviewSummary
        );

        githubRestClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}/reviews", owner, repo, pullRequestNumber)
                .contentType(MediaType.APPLICATION_JSON)
                .body(reviewRequest)
                .retrieve()
                .body(Map.class);
    }

    public record GitHubWebhookPayload(String action, GitHubPullRequest pull_request, GitHubRepository repository) {
    }

    public record GitHubPullRequest(Integer number, String title, String body) {
    }

    public record GitHubRepository(String full_name) {
    }

    public record GitHubFile(String filename, String status, String patch) {
    }
}
