package com.codekitchen.codereviewer.model;


import java.time.Instant;
import java.util.List;

import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter 
@Getter 
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "user_reviews_history")
public class ReviewDocument {

    @Id
    private String id;
 
    private String userId;
    private Instant createdAt;
    private Instant updatedAt;
    private String userProgressSummary;
    private List<PRReviewSchema> prReviewSchemas;

}
