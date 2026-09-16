package com.codekitchen.codereviewer.component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.data.repository.init.ResourceReader;
import org.springframework.stereotype.Component;

import com.codekitchen.codereviewer.component.GithubClient.GitHubFile;
import com.codekitchen.codereviewer.model.*;
import com.codekitchen.codereviewer.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component 
public class GeminiChatClient {
    
    private final GoogleGenAiChatModel googleGenAiChatModel;

    private static final Logger log = LoggerFactory.getLogger(GeminiChatClient.class);

    private final ReviewRepository reviewRepository;
    public GeminiChatClient(GoogleGenAiChatModel chatModel, ReviewRepository reviewRepository){
        this.googleGenAiChatModel = chatModel;
        this.reviewRepository = reviewRepository;
    } 

    public GenAIReviewSchema reviewPullRequest(ReviewPayload payload, List<GitHubFile> files) throws JsonProcessingException{
        Prompt prompt = buildReviewPrompt(payload, files);
        ChatResponse response = googleGenAiChatModel.call(prompt);
        String rawResponse = response.getResult().getOutput().getText();
        GenAIReviewSchema jsonResponse = new ObjectMapper().readValue(rawResponse, GenAIReviewSchema.class);
        return jsonResponse;
    }

private Prompt buildReviewPrompt(ReviewPayload payload, List<GitHubFile> files) {
        String expectedJsonFormat = "";
        try (InputStream inputStream = ResourceReader.class.getClassLoader().getResourceAsStream("genai_review_response_structure.json")) {
            // Read all bytes and convert directly to a UTF-8 String
            expectedJsonFormat = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String historyContext = "";
        try{
        Optional<ReviewDocument> maybeDoc = reviewRepository.findByUserId(payload.getPullRequestUserLogin());
        historyContext = buildHistoricalContext(maybeDoc);
        } catch (Exception e) {
            log.error("Unable to fetch historical context for " + payload.getPullRequestUserLogin(), e);
            historyContext = "Encountered an error while fetch historical context. Proceed to review without context";
        }

        StringBuilder prDetails = new StringBuilder();
        prDetails.append("Repository: ").append(payload.getRepositoryFullName()).append("\n");
        prDetails.append("Pull request number: #").append(payload.getPullRequestNumber()).append("\n");
        prDetails.append("Title: ").append(payload.getPullRequestTitle() == null ? "" : payload.getPullRequestTitle()).append("\n");

        if (payload.getPullRequestBody() != null && !payload.getPullRequestBody().isBlank()) {
            prDetails.append("Description: ").append(payload.getPullRequestBody()).append("\n");
        }

        for (GitHubFile file : files) {
            if (file.patch() == null || file.patch().isBlank()) {
                continue;
            }

            prDetails.append("FILE: ").append(file.filename()).append("\n");
            prDetails.append("STATUS: ").append(file.status() == null ? "updated" : file.status()).append("\n");
            prDetails.append("```diff\n").append(file.patch()).append("\n```\n\n");
        }

        String promptTemplateText = """
            You are an expert AI code reviewer. Perform a detailed code review on the provided Pull Request diff.

            ### USER HISTORICAL CONTEXT & PERFORMANCE TRAJECTORY
            {historyContext}

            ### CURRENT PULL REQUEST DETAILS
            {prDetails}

            ### INSTRUCTIONS
            1. Analyze the current PR diff for correctness, quality, security, and maintainability.
            2. Evaluate the user's progress by comparing patterns in this PR against their past recorded Strengths and Weaknesses.
            3. Fill out the 'userProgress' field with insights on whether past weaknesses were resolved or are recurring.
            4. Return output strictly as JSON matching the schema below (Do NOT enclose in markdown code fences).
            5. Comments section is mandatory as it will contain the actual review comments for the files
            6. Focus on likely bugs, edge cases, security concerns, code quality, and maintainability

            {expectedJsonFormat}
            """;

        PromptTemplate template = new PromptTemplate(promptTemplateText);
        Prompt prompt = template.create(Map.of(
                "historyContext", historyContext,
                "prDetails", prDetails,
                "expectedJsonFormat", expectedJsonFormat
        ));
        log.info("Prompt for GenAI is ready");
        return prompt;
    }

    private String buildHistoricalContext(Optional<ReviewDocument> maybeDoc) {
        if (maybeDoc.isEmpty()) {
            return "No prior PR Reviews are available for this user";
        }
        StringBuilder context = new StringBuilder();
        ReviewDocument doc = maybeDoc.get();

            if (doc.getUserProgressSummary() != null && !doc.getUserProgressSummary().isBlank()) {
                context.append("- Overall High-Level Summary: ").append(doc.getUserProgressSummary()).append("\n");
            }

            if (doc.getPrReviewSchemas() != null) {
                int reviewIndex = 1;
                for (PRReviewSchema pr : doc.getPrReviewSchemas()) {
                    context.append(String.format("  [Past PR #%d]\n", reviewIndex++));
                    
                    if (pr.getStrengths() != null && !pr.getStrengths().isEmpty()) {
                        context.append("    Past Strengths: ").append(String.join(", ", pr.getStrengths())).append("\n");
                    }
                    if (pr.getWeaknesses() != null && !pr.getWeaknesses().isEmpty()) {
                        context.append("    Past Weaknesses: ").append(String.join(", ", pr.getWeaknesses())).append("\n");
                    }
                    if (pr.getReviewSummary() != null) {
                        context.append("    PR Summary: ").append(pr.getReviewSummary()).append("\n");
                    }
                }
            }


        return context.toString();
    }
}
