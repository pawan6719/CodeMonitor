package com.codekitchen.codereviewer.model;

import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
@AllArgsConstructor 
public class PRReviewSchema {
    String reviewSummary;
    String pullRequest;
    String linesOfCode;
    String codeCoverage;
    String securityScore;
    String performanceScore;
    String readabilityScore;
    String overallScore;
    List<String> strengths;
    List<String> weaknesses;
    private Instant createdAt;
}
