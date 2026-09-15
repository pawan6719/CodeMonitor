package com.codekitchen.codereviewer.component;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.codekitchen.codereviewer.model.Comment;
import com.codekitchen.codereviewer.model.GenAIReviewSchema;

@Component
public class GithubClient {

        private final RestClient restClient;
        private static final Logger log = LoggerFactory.getLogger(GithubClient.class);

        public GithubClient(
                        @Value("${github.api.url:https://api.github.com}") String githubApiUrl,
                        @Value("${github.token:}") String githubToken) {

                // Java 21 native HttpClient with HTTP/2 and buffered requests (avoids GCP
                // Egress / GitHub chunked transfer broken pipe)
                HttpClient httpClient = HttpClient.newBuilder()
                                .version(HttpClient.Version.HTTP_2)
                                .connectTimeout(Duration.ofSeconds(10))
                                .build();

                JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
                requestFactory.setReadTimeout(Duration.ofSeconds(30));

                RestClient.Builder githubBuilder = RestClient.builder()
                                .requestFactory(requestFactory)
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
                                .uri("/repos/{owner}/{repo}/pulls/{pullNumber}/files", owner, repo,
                                                pullRequestNumber)
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

                // 2. Map your internal comment array to GitHub line-level comments (skipping
                // invalid line numbers or null comments)
                List<GithubLineComment> githubComments = new ArrayList<>();
                if (payload.getComments() != null) {
                        for (Comment c : payload.getComments()) {
                                if (c == null || c.getFile() == null || c.getLine() == null) {
                                        continue;
                                }
                                try {
                                        int lineNumber = Integer.parseInt(c.getLine().trim());
                                        githubComments.add(new GithubLineComment(
                                                        c.getFile(),
                                                        lineNumber,
                                                        "RIGHT", // Places comment on the newly introduced/modified code
                                                                 // line
                                                        String.format("**[%s]** %s\n\n*Suggestion:* %s",
                                                                        c.getSeverity(),
                                                                        c.getComment(), c.getSuggestion())));
                                } catch (NumberFormatException e) {
                                        log.warn("Skipping comment on file {} due to invalid line number: {}",
                                                        c.getFile(), c.getLine());
                                }
                        }
                }

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
