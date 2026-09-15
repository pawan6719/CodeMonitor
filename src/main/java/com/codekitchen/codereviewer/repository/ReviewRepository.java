package com.codekitchen.codereviewer.repository;

import com.codekitchen.codereviewer.model.ReviewDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<ReviewDocument, String> {
    Optional<ReviewDocument> findByUserId(String userId);
    Optional<ReviewDocument> findTopByUserIdOrderByCreatedAtDesc(String userId);
}

