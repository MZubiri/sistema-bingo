package com.bingo.backend.card;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface IdempotencyRequestRepository extends JpaRepository<IdempotencyRequest, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select r from IdempotencyRequest r
            left join fetch r.card
            where r.user.id = :userId and r.idempotencyKey = :key
            """)
    Optional<IdempotencyRequest> findByUserAndKeyForUpdate(@Param("userId") Long userId, @Param("key") String key);
}
