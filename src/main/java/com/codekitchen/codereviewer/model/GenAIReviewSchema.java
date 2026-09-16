package com.codekitchen.codereviewer.model;

import java.util.List;

import lombok.Data;

@Data 
public class GenAIReviewSchema {
    private String projectId;
    private String projectName;
    private String pullRequestNumber;
    private String pullRequestTitle;
    private String pullRequestDescription;
    private String pullRequestAuthor;
    private String commitId;
    private String reviewer;
    private String summary;
    private Metrics metrics;
    List<Comment> comments;
    private String overallScore;
    private List<String> strengths;
    private List<String> weaknesses;
    private String userProgress;
}

