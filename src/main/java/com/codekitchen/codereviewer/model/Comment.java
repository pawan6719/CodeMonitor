package com.codekitchen.codereviewer.model;

import lombok.Data;

@Data
public class Comment {
    private String file;
    private String line;
    private String comment;
    private Severity severity;
    private String suggestion;
}
