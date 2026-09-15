package com.codekitchen.codereviewer.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.codekitchen.codereviewer.model.GenAIReviewSchema;
import com.codekitchen.codereviewer.service.ReviewService;

@Component
public class GithubClient {

        private final RestClient restClient;
        private static final Logger log = LoggerFactory.getLogger(GithubClient.class);

        public GithubClient(
                        @Value("${github.api.url:https://api.github.com}") String githubApiUrl,
                        @Value("${github.token:}") String githubToken) {
                RestClient.Builder githubBuilder = RestClient.builder()
                                .requestFactory(new HttpComponentsClientHttpRequestFactory()) 
                                .baseUrl(githubApiUrl)
                                .defaultHeader("Accept", "application/vnd.github+json")
                                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                                .defaultHeader("User-Agent", "CodeMonitor");

                if (githubToken != null && !githubToken.isBlank()) {
                        githubBuilder = githubBuilder.defaultHeader("Authorization", "Bearer " + githubToken);
                }

                this.restClient = githubBuilder.build();
        }

        public List<GitHubFile> fetchPullRequestFiles(String owner, String repo, int pullRequestNumber) {
                List<GitHubFile> files = restClient.get()
                                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}/files", owner, repo, pullRequestNumber)
                                .retrieve()
                                .body(new ParameterizedTypeReference<>() {
                                });
                log.info(files == null ? "Error retrieving Files" : "Retrieved files count " + files.size());
                return files == null ? new ArrayList<>() : files;
        }

        public String postPullRequestReview(String owner, String repo, String commitId, GenAIReviewSchema payload) {
                // 1. Build the main summary body string combining metadata
                String overallSummary = String.format(
                                "%s\n\n### Metrics Summary\n* **Overall Score:** %s/10\n* **Security:** %s\n* **Performance:** %s\n\n### Progress\n%s",
                                payload.getSummary(),
                                payload.getOverallScore(),
                                payload.getMetrics().getSecurityScore(),
                                payload.getMetrics().getPerformanceScore(),
                                payload.getUserProgress());

                // 2. Map your internal comment array to GitHub line-level comments
                List<GithubLineComment> githubComments = payload.getComments().stream()
                                .map(c -> new GithubLineComment(
                                                c.getFile(),
                                                Integer.parseInt(c.getLine()),
                                                "RIGHT", // Places comment on the newly introduced/modified code line
                                                String.format("**[%s]** %s\n\n*Suggestion:* %s", c.getSeverity(),
                                                                c.getComment(), c.getSuggestion())))
                                .collect(Collectors.toList());

                // 3. Construct the collective payload
                GithubReviewRequest gitHubPayload = new GithubReviewRequest(
                                overallSummary,
                                "COMMENT", // Can be "COMMENT", "APPROVE", or "REQUEST_CHANGES"
                                commitId,
                                githubComments);

                log.info("Overall Summary for the review is " + overallSummary);

                // 4. Fire POST to GitHub
                return this.restClient.post()
                                .uri("/repos/{owner}/{repo}/pulls/{prNumber}/reviews", owner, repo,
                                                payload.getPullRequestNumber())
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(gitHubPayload)
                                .retrieve()
                                .body(String.class);
        }

        public record GithubReviewRequest(
                        String body,
                        String event,
                        String commit_id,
                        List<GithubLineComment> comments) {
        }

        public record GithubLineComment(
                        String path,
                        int line,
                        String side,
                        String body) {
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
