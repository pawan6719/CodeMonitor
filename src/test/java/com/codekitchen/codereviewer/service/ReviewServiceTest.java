package com.codekitchen.codereviewer.service;

import com.codekitchen.codereviewer.component.*;
import com.codekitchen.codereviewer.component.GithubClient.GitHubFile;
import com.codekitchen.codereviewer.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ReviewServiceTest {

    private ReviewService reviewService;
    private GithubClient githubClient;
    private GeminiChatClient geminiChatClient;
    private ReviewPersistenceService reviewPersistenceService;

    @BeforeEach
    void setUp() throws Exception {

        githubClient = Mockito.mock(GithubClient.class);
        geminiChatClient = Mockito.mock(GeminiChatClient.class);
        reviewPersistenceService = Mockito.mock(ReviewPersistenceService.class);
        reviewService = new ReviewService(
               geminiChatClient, githubClient, reviewPersistenceService
        );
    }

    // @Test
    // void reviewPullRequest_shouldFetchFilesPostReviewAndReturnSummary() throws Exception {
    //     ReviewPayload payload = readPayload("pull_request_valid.json");

    //     Mockito.when(githubClient.fetchPullRequestFiles("", "", 2))
    //     .thenReturn(new ArrayList<GitHubFile>());

    //     reviewService.reviewPullRequest(payload);
    // }

    private ReviewPayload readPayload(String resourceName) throws Exception {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        Path path = Paths.get(resourceUrl.toURI());
        String payload = Files.readString(path);
        Map<String, Object> mapPayload = new HashMap<String, Object>();
        mapPayload = new ObjectMapper().readValue(payload, Map.class);
        ReviewPayload reviewPayload = new ReviewPayload();
        reviewPayload.setPayload(mapPayload);
        return reviewPayload;
    }
}
