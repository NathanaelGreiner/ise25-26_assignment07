package de.seuhd.campuscoffee.data.persistence.repositories;

import de.seuhd.campuscoffee.data.persistence.entities.PosEntity;
import de.seuhd.campuscoffee.data.persistence.entities.ReviewEntity;
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity;
import de.seuhd.campuscoffee.domain.model.objects.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for persisting review entities.
 */
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long>, ResettableSequenceRepository {
    List<ReviewEntity> findAllByPosAndApproved(PosEntity pos, Boolean approved);
    List<ReviewEntity> findAllByPosAndAuthor(PosEntity pos, UserEntity author);

    Optional<ReviewEntity> findByAuthor(User authorAsUser);
}
