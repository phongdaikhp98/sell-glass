package com.sellglass.review.dto;

import com.sellglass.review.Review;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ReviewResponse {

    private UUID id;
    private UUID customerId;
    private String customerName;
    private short rating;
    private String comment;
    private LocalDateTime createdAt;

    public static ReviewResponse from(Review review, String customerName) {
        ReviewResponse r = new ReviewResponse();
        r.id = review.getId();
        r.customerId = review.getCustomerId();
        r.customerName = customerName;
        r.rating = review.getRating();
        r.comment = review.getComment();
        r.createdAt = review.getCreatedAt();
        return r;
    }
}
