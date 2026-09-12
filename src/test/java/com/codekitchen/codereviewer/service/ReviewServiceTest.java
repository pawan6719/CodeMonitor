package com.codekitchen.codereviewer.service;

import com.codekitchen.codereviewer.model.ReviewPayload;
import com.codekitchen.codereviewer.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

class ReviewServiceTest {

    private MockWebServer githubServer;
    private MockWebServer geminiServer;
    private ReviewService reviewService;

    @BeforeEach
    void setUp() throws Exception {
        githubServer = new MockWebServer();
        geminiServer = new MockWebServer();
        githubServer.start();
        geminiServer.start();

        String githubBaseUrl = githubServer.url("/").toString();
        String geminiBaseUrl = geminiServer.url("/").toString();

        reviewService = new ReviewService(
                githubBaseUrl,
                "test-token",
                geminiBaseUrl,
                "test-gemini-key",
                "gemini-2.0-flash"
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        githubServer.shutdown();
        geminiServer.shutdown();
    }

    // @Test
    // void reviewPullRequest_shouldFetchFilesPostReviewAndReturnSummary() throws Exception {
    //     ReviewPayload payload = readPayload("pull_request_valid.json");

    //     githubServer.enqueue(new MockResponse()
    //             .setResponseCode(200)
    //             .setHeader("Content-Type", "application/json")
    //             .setBody("""
    //                 [
    //                   {
    //                     "filename": "src/main/java/com/codekitchen/codereviewer/controller/ReviewController.java",
    //                     "status": "modified",
    //                     "patch": "@@\n+test\n"
    //                   }
    //                 ]
    //                 """));

    //     githubServer.enqueue(new MockResponse()
    //             .setResponseCode(200)
    //             .setHeader("Content-Type", "application/json")
    //             .setBody("{}"));

    //     geminiServer.enqueue(new MockResponse()
    //             .setResponseCode(200)
    //             .setHeader("Content-Type", "application/json")
    //             .setBody("""
    //                 {
    //                   "candidates": [
    //                     {
    //                       "content": {
    //                         "parts": [
    //                           { "text": "Suggested review summary" }
    //                         ]
    //                       }
    //                     }
    //                   ]
    //                 }
    //                 """));

    //     String result = reviewService.reviewPullRequest(payload);

    //     assertEquals("Suggested review summary", result);
    //     assertEquals(2, githubServer.getRequestCount());
    //     assertEquals(1, geminiServer.getRequestCount());
    // }

    @Test
    void reviewPushRequest_shouldBuildPromptAndReturnGeminiSummary() throws Exception {
        ReviewPayload payload = readPayload("push_request_valid.json");

        geminiServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              { "text": "Push review summary" }
                            ]
                          }
                        }
                      ]
                    }
                    """));

        String result = reviewService.reviewPushRequest(payload);

        assertEquals("Push review summary", result);
        assertEquals(1, geminiServer.getRequestCount());
    }

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
