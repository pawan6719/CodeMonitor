package com.codekitchen.codereviewer.model;

import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter
public class Metrics {
    private String linesOfCode;
    private String codeCoverage;
    private String securityScore;
    private String performanceScore;
    private String readabilityScore;
}
