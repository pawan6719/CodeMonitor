package com.codekitchen.codereviewer.service;

import com.codekitchen.codereviewer.model.GenAIReviewSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.codekitchen.codereviewer.model.ReviewPayload;
import com.codekitchen.codereviewer.component.GeminiChatClient;
import com.codekitchen.codereviewer.component.GithubClient;
import com.codekitchen.codereviewer.component.GithubClient.GitHubFile;

import java.util.List;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final GithubClient githubRestClient;
    private final GeminiChatClient chatClient;
    private final ReviewPersistenceService reviewPersistenceService;

    public ReviewService(
            GeminiChatClient chatClient,
            GithubClient githubRestClient,
            ReviewPersistenceService reviewPersistenceService) {

        this.reviewPersistenceService = reviewPersistenceService;
        this.chatClient = chatClient;
        this.githubRestClient = githubRestClient;
    }

    @Async
    public void reviewPullRequest(ReviewPayload payload) {
        if (payload == null || payload.getPullRequest() == null) {
            throw new IllegalArgumentException("Pull request payload is missing.");
        }

        if (payload.getRepository() == null || payload.getRepositoryFullName() == null
                || payload.getRepositoryFullName().isBlank()) {
            throw new IllegalArgumentException("Repository details are missing from the webhook payload.");
        }

        if (payload.getPullRequestNumber() == null) {
            throw new IllegalArgumentException("Pull request number is missing from the webhook payload.");
        }

        String[] repositoryParts = payload.getRepositoryFullName().split("/", 2);

        if (repositoryParts.length != 2) {
            throw new IllegalArgumentException("Repository full name must be in owner/repo format.");
        }

        String owner = repositoryParts[0];
        String repo = repositoryParts[1];

        List<GitHubFile> files = githubRestClient.fetchPullRequestFiles(owner, repo, payload.getPullRequestNumber());

        if (files.isEmpty()) {
            throw new RuntimeException(
                    "No changed files were found for pull request #" + payload.getPullRequestNumber() + ".");
        }

        GenAIReviewSchema review = chatClient.reviewPullRequest(payload, files);
        String commitId = payload.getHeadSha();
        log.info(String.format("AI Code reviewer has completed review for repo %s, pull_request number %s with overall Score = %s", review.getProjectId(), review.getPullRequestNumber(), review.getOverallScore()));
        log.info(review.toString());
        log.info("Commit Id is - " + commitId);
        githubRestClient.postPullRequestReview(owner, repo, commitId, review);

        if (reviewPersistenceService != null) {
            reviewPersistenceService.saveReview(payload, "pull_request", review);
        }
    }

}
