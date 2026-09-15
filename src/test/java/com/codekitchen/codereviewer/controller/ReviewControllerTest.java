package com.codekitchen.codereviewer.controller;

import com.codekitchen.codereviewer.controller.ReviewController;
import com.codekitchen.codereviewer.model.GenAIReviewSchema;
import com.codekitchen.codereviewer.model.ReviewPayload;
import com.codekitchen.codereviewer.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.*;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean 
    private ReviewService reviewService;

    // @Test
    // void reviewPull_shouldReturnOkWithValidPullRequestPayload() throws Exception {
    //     URL resourceUrl = getClass().getClassLoader().getResource("pull_request_valid.json");
    //     Path path = Paths.get(resourceUrl.toURI());
    //     String payload = Files.readString(path);

    //     when(reviewService.reviewPullRequest(ArgumentMatchers.any(ReviewPayload.class)))
    //             .thenReturn(new GenAIReviewSchema());

    //     mockMvc.perform(post("/api/review/pull")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .header("X-GitHub-Event", "pull_request")
    //                     .content(payload))
    //             .andExpect(status().isOk())
    //             .andExpect(content().string("Mocked pull request review summary"));
    // }

    // @Test
    // void reviewPush_shouldReturnOkWithValidPushPayload() throws Exception {
    //     URL resourceUrl = getClass().getClassLoader().getResource("push_request_valid.json");
    //     Path path = Paths.get(resourceUrl.toURI());
    //     String payload = Files.readString(path);
    //     //String payload = readResource("resources/push_request_valid.json");

    //     when(reviewService.reviewPushRequest(ArgumentMatchers.any(ReviewPayload.class)))
    //             .thenReturn("Mocked push review summary");

    //     mockMvc.perform(post("/api/review/push")
    //                     .contentType(MediaType.APPLICATION_JSON)
    //                     .header("X-GitHub-Event", "push")
    //                     .content(payload))
    //             .andExpect(status().isOk())
    //             .andExpect(content().string("Mocked push review summary"));
    // }

    private String readResource(String resourceName) throws Exception {
        ClassPathResource resource = new ClassPathResource(resourceName, getClass());
        return resource.getContentAsString(StandardCharsets.UTF_8);
    }
}
