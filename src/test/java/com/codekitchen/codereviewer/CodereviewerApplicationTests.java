package com.codekitchen.codereviewer;

import com.codekitchen.codereviewer.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class CodereviewerApplicationTests {

    @MockitoBean
    private GoogleGenAiChatModel googleGenAiChatModel;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @Test
    void contextLoads() {
        assertNotNull(googleGenAiChatModel);
        assertNotNull(reviewRepository);
    }
}

