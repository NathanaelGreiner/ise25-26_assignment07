package de.seuhd.campuscoffee.api.dtos;

import de.seuhd.campuscoffee.domain.model.objects.Pos;
import de.seuhd.campuscoffee.domain.model.objects.User;
import lombok.Builder;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * DTO record for Review metadata.
 */
@Builder(toBuilder = true)
public record ReviewDto (
    @Nullable Long id,
    @Nullable LocalDateTime createdAt,
    @Nullable LocalDateTime updatedAt,

    @NotNull
    @Size(min = 1, max = 16386, message = "review must be between 1 and 16386 characters long.")
    @Pattern(regexp = "\\w+", message = "Review can only contain word characters: [a-zA-Z_0-9]+")
    @NonNull String review,

    @NonNull Integer approvalCount,
    @NonNull Boolean approved,
    @NonNull Long posId,
    @NonNull Long authorId

) implements Dto<Long> {
    @Override
    public @Nullable Long getId() {
        return id;
    }
}
