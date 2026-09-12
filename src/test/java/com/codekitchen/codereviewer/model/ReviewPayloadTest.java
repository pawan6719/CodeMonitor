package com.codekitchen.codereviewer.model;

import com.codekitchen.codereviewer.model.ReviewPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReviewPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializePullRequestWebhookPayloadIntoReviewPayload() throws Exception {
        String json = readResource("pull_request_valid.json");

        Map<String, Object> reviewPayload = objectMapper.readValue(json, Map.class);
        ReviewPayload payload = new ReviewPayload();
        payload.setPayload(reviewPayload);
        
        assertNotNull(payload);
        assertNotNull(payload.getRepository());
        assertEquals("pawan6719/CodeMonitor", payload.getRepositoryFullName());
        assertNotNull(payload.getPullRequest());
        assertEquals(3, payload.getPullRequestNumber());
        assertEquals("Dev", payload.getPullRequestTitle());
        assertEquals("29fd92e99337815f4563e85c4356bc3baecbe6df", payload.getHeadSha());
        assertEquals("3bc5b77cda793a3392430c724a689c19ed9c8d46", payload.getBaseSha());
    }

    @Test
    void shouldDeserializePushWebhookPayloadIntoReviewPayload() throws Exception {
        String json = readResource("push_request_valid.json");

        Map<String, Object> reviewPayload = objectMapper.readValue(json, Map.class);
        ReviewPayload payload = new ReviewPayload();
        payload.setPayload(reviewPayload);
        assertNotNull(payload);
        assertNotNull(payload.getRepository());
        assertEquals("pawan6719/CodeMonitor", payload.getRepositoryFullName());
        assertEquals("refs/heads/feature/request-payload-testing", payload.getRef());
        assertEquals("https://github.com/pawan6719/CodeMonitor/compare/feature/request-payload-testing", payload.getCompareUrl());
        assertNotNull(payload.getPusher());
        assertEquals("pawan6719", payload.getPusherName());
        assertEquals("pawan6719@users.noreply.github.com", payload.getPusherEmail());
        assertEquals(1, payload.getModifiedFiles().size());
        assertEquals("src/main/java/com/codekitchen/codereviewer/controller/ReviewController.java", payload.getModifiedFiles().get(0));
    }

    private String readResource(String resourceName) throws Exception {
        URL resourceUrl = getClass().getClassLoader().getResource(resourceName);
        Path path = Paths.get(resourceUrl.toURI());
        String payload = Files.readString(path);
        return payload;
    }
}
