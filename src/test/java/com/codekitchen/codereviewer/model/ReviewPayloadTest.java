package com.codekitchen.codereviewer.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should deserialize pull_request_valid.json and verify all PR-related getters")
    void shouldDeserializePullRequestWebhookPayloadIntoReviewPayload() throws Exception {
        String json = readResource("pull_request_valid.json");

        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        ReviewPayload payload = new ReviewPayload(map);

        assertNotNull(payload);
        assertEquals("opened", payload.getAction());
        assertNotNull(payload.getRepository());
        assertEquals("CodeMonitor", payload.getRepositoryName());
        assertEquals("pawan6719/CodeMonitor", payload.getRepositoryFullName());
        assertEquals("pawan6719", payload.getRepositoryOwnerLogin());

        assertNotNull(payload.getPullRequest());
        assertEquals(3, payload.getPullRequestNumber());
        assertEquals("Dev", payload.getPullRequestTitle());
        assertEquals("pawan6719", payload.getPullRequestUserLogin());
        assertNull(payload.getPullRequestBody());
        assertEquals("https://github.com/pawan6719/CodeMonitor/pull/3", payload.getPullRequestHtmlUrl());
        assertEquals("open", payload.getPullRequestState());
        assertEquals("29fd92e99337815f4563e85c4356bc3baecbe6df", payload.getHeadSha());
        assertEquals("3bc5b77cda793a3392430c724a689c19ed9c8d46", payload.getBaseSha());
    }

    @Test
    @DisplayName("Should deserialize push_request_valid.json and verify push-related getters")
    void shouldDeserializePushWebhookPayloadIntoReviewPayload() throws Exception {
        String json = readResource("push_request_valid.json");

        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        ReviewPayload payload = new ReviewPayload();
        payload.setPayload(map);

        assertNotNull(payload);
        assertEquals("refs/heads/feature/request-payload-testing", payload.getRef());
        assertEquals("0000000000000000000000000000000000000000", payload.getBefore());
        assertEquals("3bc5b77cda793a3392430c724a689c19ed9c8d46", payload.getAfter());
        assertNotNull(payload.getRepository());
        assertEquals("pawan6719/CodeMonitor", payload.getRepositoryFullName());
        assertEquals("https://github.com/pawan6719/CodeMonitor/compare/feature/request-payload-testing", payload.getCompareUrl());

        assertNotNull(payload.getPusher());
        assertEquals("pawan6719", payload.getPusherName());
        assertEquals("pawan6719@users.noreply.github.com", payload.getPusherEmail());

        assertNotNull(payload.getSender());
        assertEquals("pawan6719", payload.getSenderLogin());

        assertNotNull(payload.getCommits());
        assertEquals(0, payload.getCommits().size());
        assertEquals(1, payload.getModifiedFiles().size());
        assertEquals("src/main/java/com/codekitchen/codereviewer/controller/ReviewController.java", payload.getModifiedFiles().get(0));
    }

    @Test
    @DisplayName("Should handle empty payload and return null or empty collections safely")
    void shouldHandleEmptyPayloadGracefully() {
        ReviewPayload payload = new ReviewPayload(null);

        assertNull(payload.getAction());
        assertNull(payload.getRef());
        assertNull(payload.getBefore());
        assertNull(payload.getAfter());
        assertNull(payload.getBaseRef());
        assertNotNull(payload.getRepository());
        assertTrue(payload.getRepository().isEmpty());
        assertNull(payload.getRepositoryFullName());
        assertNull(payload.getRepositoryName());
        assertNull(payload.getRepositoryOwnerLogin());
        assertNotNull(payload.getPullRequest());
        assertTrue(payload.getPullRequest().isEmpty());
        assertNull(payload.getPullRequestNumber());
        assertNull(payload.getPullRequestTitle());
        assertNull(payload.getPullRequestBody());
        assertNull(payload.getPullRequestHtmlUrl());
        assertNull(payload.getPullRequestState());
        assertNull(payload.getHeadSha());
        assertNull(payload.getBaseSha());
        assertNull(payload.getCompareUrl());
        assertNotNull(payload.getCommits());
        assertTrue(payload.getCommits().isEmpty());
        assertNotNull(payload.getModifiedFiles());
        assertTrue(payload.getModifiedFiles().isEmpty());
        assertNotNull(payload.getSender());
        assertTrue(payload.getSender().isEmpty());
        assertNull(payload.getSenderLogin());
        assertNotNull(payload.getPusher());
        assertTrue(payload.getPusher().isEmpty());
        assertNull(payload.getPusherName());
        assertNull(payload.getPusherEmail());
    }

    @Test
    @DisplayName("Should test dynamic field setters and conversion helper branches")
    void shouldTestDynamicFieldAndHelpers() {
        ReviewPayload payload = new ReviewPayload();
        payload.setDynamicField("base_ref", "main");
        payload.setDynamicField("customNumber", 42);
        assertEquals("main", payload.getBaseRef());
        assertEquals(42, payload.getPayload().get("customNumber"));

        // Test PR number from string representation
        Map<String, Object> prMap = new HashMap<>();
        prMap.put("number", "101");
        prMap.put("head", Map.of("sha", "head123"));
        prMap.put("base", Map.of("sha", "base123"));
        payload.setDynamicField("pull_request", prMap);
        assertEquals(101, payload.getPullRequestNumber());
        assertEquals("head123", payload.getHeadSha());
        assertEquals("base123", payload.getBaseSha());

        // Test PR number with invalid string
        prMap.put("number", "not-a-number");
        assertNull(payload.getPullRequestNumber());

        // Test modified files with head_commit
        Map<String, Object> headCommit = new HashMap<>();
        headCommit.put("modified", List.of("FileA.java", "FileB.java"));
        payload.setDynamicField("head_commit", headCommit);
        assertEquals(2, payload.getModifiedFiles().size());
        assertEquals("FileA.java", payload.getModifiedFiles().get(0));

        // Test repository with missing owner
        Map<String, Object> repoMap = new HashMap<>();
        repoMap.put("name", "TestRepo");
        payload.setDynamicField("repository", repoMap);
        assertEquals("TestRepo", payload.getRepositoryName());
        assertNull(payload.getRepositoryOwnerLogin());
    }

    private String readResource(String resourceName) throws Exception {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        Path path = Paths.get(resourceUrl.toURI());
        return Files.readString(path);
    }
}

