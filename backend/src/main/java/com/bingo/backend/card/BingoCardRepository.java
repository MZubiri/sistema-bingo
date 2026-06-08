package com.bingo.backend.card;

import com.bingo.backend.common.OrganizationCode;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BingoCardRepository extends JpaRepository<BingoCard, Long> {
    boolean existsBySerial(String serial);

    boolean existsByPositionalSignature(String positionalSignature);

    @EntityGraph(attributePaths = "assignedTo")
    Optional<BingoCard> findBySerial(String serial);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = "assignedTo")
    @Query("select c from BingoCard c where c.serial = :serial")
    Optional<BingoCard> findBySerialForUpdate(@Param("serial") String serial);

    @Override
    @EntityGraph(attributePaths = "assignedTo")
    List<BingoCard> findAll();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from BingoCard c
            where c.organizationCode = :organizationCode
              and c.status = :status
            order by c.serial asc
            """)
    List<BingoCard> findNextAvailableForUpdate(
            @Param("organizationCode") OrganizationCode organizationCode,
            @Param("status") CardStatus status,
            Pageable pageable
    );

    long countByStatus(CardStatus status);

    long countByOrganizationCode(OrganizationCode organizationCode);

    long countByOrganizationCodeAndStatus(OrganizationCode organizationCode, CardStatus status);

    @EntityGraph(attributePaths = "assignedTo")
    List<BingoCard> findByAssignedToIdOrderByAssignedAtDesc(Long userId);

    @EntityGraph(attributePaths = "assignedTo")
    @Query("""
            select c from BingoCard c
            left join c.assignedTo assignedTo
            where (:search is null
                or lower(c.serial) like lower(concat('%', :search, '%'))
                or lower(c.buyerName) like lower(concat('%', :search, '%'))
                or lower(assignedTo.fullName) like lower(concat('%', :search, '%')))
              and (:organizationCode is null or c.organizationCode = :organizationCode)
              and (:status is null or c.status = :status)
              and (:assignedFrom is null or c.assignedAt >= :assignedFrom)
              and (:assignedTo is null or c.assignedAt < :assignedTo)
            order by c.serial asc
            """)
    List<BingoCard> searchAdminCards(
            @Param("search") String search,
            @Param("organizationCode") OrganizationCode organizationCode,
            @Param("status") CardStatus status,
            @Param("assignedFrom") java.time.Instant assignedFrom,
            @Param("assignedTo") java.time.Instant assignedTo
    );

    @EntityGraph(attributePaths = "assignedTo")
    @Query("""
            select c from BingoCard c
            where c.assignedTo.id = :userId
              and (:search is null
                or lower(c.serial) like lower(concat('%', :search, '%'))
                or lower(c.buyerName) like lower(concat('%', :search, '%')))
              and (:status is null or c.status = :status)
              and (:assignedFrom is null or c.assignedAt >= :assignedFrom)
              and (:assignedTo is null or c.assignedAt < :assignedTo)
            order by c.assignedAt desc
            """)
    List<BingoCard> searchHistory(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("status") CardStatus status,
            @Param("assignedFrom") java.time.Instant assignedFrom,
            @Param("assignedTo") java.time.Instant assignedTo
    );

    @Query("""
            select c from BingoCard c
            where c.organizationCode = :organizationCode
              and c.status = :status
              and (:search is null or c.serial like concat('%', :search, '%'))
            order by c.serial asc
            """)
    Page<BingoCard> searchAvailable(
            @Param("organizationCode") OrganizationCode organizationCode,
            @Param("status") CardStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
