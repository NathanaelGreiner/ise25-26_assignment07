package de.seuhd.campuscoffee.domain.model.objects;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

/**
 * Domain record that stores a review for a point of sale.
 * Reviews are approved once they received a configurable number of approvals.
 */
@Builder(toBuilder = true)
public record Review(
        @Nullable Long id, // null when the review has not been created yet
        @NonNull String review,
        @NonNull User author,
        @NonNull Pos pos,
        @Nullable LocalDateTime createdAt,
        @Nullable LocalDateTime updatedAt,
        @NonNull Integer approvalCount, // is updated by the domain module
        @NonNull Boolean approved // is determined by the domain module
) implements DomainModel<Long> {
    @Override
    public Long getId() {
        return id;
    }
    public String review() {return review; }
    public User author() {return author; }
    public Pos pos() {return pos;}
    public Boolean approved() {return approved; }
}