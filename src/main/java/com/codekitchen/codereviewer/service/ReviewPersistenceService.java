package com.codekitchen.codereviewer.service;

import com.codekitchen.codereviewer.model.*;
import com.codekitchen.codereviewer.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ReviewPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(ReviewPersistenceService.class);

    private final ReviewRepository reviewRepository;

    public ReviewPersistenceService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    public ReviewDocument saveReview(ReviewPayload payload, String eventType, GenAIReviewSchema review) throws RuntimeException {
        if(!review.getPullRequestTitle().equals(payload.getPullRequestTitle()) || Integer.parseInt(review.getPullRequestNumber()) != payload.getPullRequestNumber())
        {
            throw new RuntimeException("Payload Pull Request does not match GenAI response");
        }
        String userId = eventType.equalsIgnoreCase(Events.PULL_REQUEST.name()) ? payload.getPullRequestUserLogin():payload.getSenderLogin();
        Optional<ReviewDocument> maybeReviewDocument = reviewRepository.findByUserId(userId);
        ReviewDocument reviewDocument = new ReviewDocument();
        List<PRReviewSchema> earlierReviews;
        if(maybeReviewDocument.isEmpty()){
            earlierReviews = new ArrayList<>();
            reviewDocument.setCreatedAt(Instant.now());
            reviewDocument.setUserId(userId);
        } else {
            // update the existing document with the incoming review for new PR. Also the updatedAt field.
            reviewDocument = maybeReviewDocument.get();
            earlierReviews = reviewDocument.getPrReviewSchemas();
        }
        earlierReviews.add(new PRReviewSchema(
                    review.getSummary(),
                    payload.getPullRequestHtmlUrl(),
                    review.getMetrics().getLinesOfCode(),
                    review.getMetrics().getCodeCoverage(),
                    review.getMetrics().getSecurityScore(),
                    review.getMetrics().getPerformanceScore(),
                    review.getMetrics().getReadabilityScore(),
                    review.getOverallScore(),
                    review.getStrengths(),
                    review.getWeaknesses(),
                    Instant.now()
            ));
            reviewDocument.setPrReviewSchemas(new ArrayList<>(earlierReviews));
            reviewDocument.setUpdatedAt(Instant.now());
            reviewDocument.setCreatedAt(Instant.now());
            reviewDocument.setUserProgressSummary(review.getUserProgress());
            reviewDocument.setUserId(userId);


        ReviewDocument saved = reviewRepository.save(reviewDocument);
        log.info("Saved review document for {} with id {}", payload.getRepositoryFullName(), saved.getId());
        return saved;
    }

    public Optional<ReviewDocument> getReview(String id) {
        return reviewRepository.findById(id);
    }
}
