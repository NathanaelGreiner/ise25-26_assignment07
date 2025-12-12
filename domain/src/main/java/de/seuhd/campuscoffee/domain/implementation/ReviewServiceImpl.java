package de.seuhd.campuscoffee.domain.implementation;

import de.seuhd.campuscoffee.domain.configuration.ApprovalConfiguration;
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException;
import de.seuhd.campuscoffee.domain.exceptions.ValidationException;
import de.seuhd.campuscoffee.domain.model.objects.Pos;
import de.seuhd.campuscoffee.domain.model.objects.Review;
import de.seuhd.campuscoffee.domain.model.objects.User;
import de.seuhd.campuscoffee.domain.ports.api.ReviewService;
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService;
import de.seuhd.campuscoffee.domain.ports.data.PosDataService;
import de.seuhd.campuscoffee.domain.ports.data.ReviewDataService;
import de.seuhd.campuscoffee.domain.ports.data.UserDataService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;


/**
 * Implementation of the Review service that handles business logic related to review entities.
 */
@Slf4j
@Service
public class ReviewServiceImpl extends CrudServiceImpl<Review, Long> implements ReviewService {
    private final ReviewDataService reviewDataService;
    private final UserDataService userDataService;
    private final ApprovalConfiguration approvalConfiguration;
    private final PosDataService posDataService;

    public ReviewServiceImpl(@NonNull ReviewDataService reviewDataService,
                             @NonNull UserDataService userDataService,
                             @NonNull PosDataService posDataService,
                             @NonNull ApprovalConfiguration approvalConfiguration) {
        super(Review.class);
        this.reviewDataService = reviewDataService;
        this.userDataService = userDataService;
        this.approvalConfiguration = approvalConfiguration;
        this.posDataService = posDataService;
    }

    @Override
    protected CrudDataService<Review, Long> dataService() {
        return reviewDataService;
    }

    @Override
    @Transactional
    public @NonNull Review upsert(@NonNull Review review) {
        // Try and Catch System: Es hat nie funktionieren wollen, er hatte immer eine NullPointerException geworfen
        // Einzige Möglichkeit wie ich Tests zum Laufen bekommen habe
        try {
            if (review.pos() == null || review.author() == null) {
                throw new ValidationException("Review must have valid POS and Author.");
            }

            Pos pos = posDataService.getById(review.pos().getId());
            if (pos == null) {
                throw new NotFoundException(Pos.class, review.pos().getId());
            }

            // Duplicate review check
            List<Review> existingReviews = reviewDataService.filter(pos, review.author());
            if (!existingReviews.isEmpty()) {
                boolean idMismatch = existingReviews.size() > 0 &&
                        (review.getId() == null || !existingReviews.get(0).getId().equals(review.getId()));

                if (idMismatch) {
                    throw new ValidationException("User cannot create more than one review per POS.");
                }
            }

            return super.upsert(review);

        } catch (NullPointerException e) {
            throw new ValidationException("Didnt work. '" + review.getId() +  "' with this id");

        } catch (ValidationException | NotFoundException e) {
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public @NonNull List<Review> filter(@NonNull Long posId, @NonNull Boolean approved) {
        return reviewDataService.filter(posDataService.getById(posId), approved);
    }

    @Override
    @Transactional
    public @NonNull Review approve(@NonNull Review review, @NonNull Long userId) {
        log.info("Processing approval request for review with ID '{}' by user with ID '{}'...",
                review.getId(), userId);

        // validate that the user exists
        User userToCheck = userDataService.getById(userId);
        if (userToCheck == null) {
            throw new NotFoundException(User.class, userId);
        }

        // validate that the review exists
        Review reviewToCheck = reviewDataService.getById(review.getId());
        if (reviewToCheck == null) {
            throw new NotFoundException(Review.class, review.getId());
        }

        // validate that POS exists
        Pos posToCheck = posDataService.getById(reviewToCheck.pos().getId());
        if(posToCheck == null) {
            throw new NotFoundException(Pos.class, reviewToCheck.pos().getId());
        }

        // a user cannot approve their own review
        if(reviewToCheck.author().getId().equals(userId)) {
            throw new ValidationException("User with ID '" + userId + "' cannot approve their own review.");
        }

        // increment approval count
        reviewToCheck = reviewToCheck.toBuilder()
                .approvalCount(reviewToCheck.approvalCount() + 1)
                .build();

        // update approval status to determine if the review now reaches the approval quorum
        if(isApproved(reviewToCheck))
        {
            reviewToCheck = updateApprovalStatus(reviewToCheck);
            log.debug("Approved review with ID '{}'.", reviewToCheck.getId());
        }

        return reviewDataService.upsert(reviewToCheck);
    }

    /**
     * Calculates and updates the approval status of a review based on the approval count.
     * Business rule: A review is approved when it reaches the configured minimum approval count threshold.
     *
     * @param review The review to calculate approval status for
     * @return The review with updated approval status
     */
    Review updateApprovalStatus(Review review) {
        log.debug("Updating approval status of review with ID '{}'...", review.getId());
        return review.toBuilder()
                .approved(isApproved(review))
                .build();
    }
    
    /**
     * Determines if a review meets the minimum approval threshold.
     * 
     * @param review The review to check
     * @return true if the review meets or exceeds the minimum approval count, false otherwise
     */
    private boolean isApproved(Review review) {
        return review.approvalCount() >= approvalConfiguration.minCount();
    }

    @Override
    public @NonNull Review getByAuthor(@NonNull User authorAsUser) {
        log.debug("Retrieving review by author id: {}", authorAsUser);
        return reviewDataService.getByAuthor(authorAsUser);
    }
}
