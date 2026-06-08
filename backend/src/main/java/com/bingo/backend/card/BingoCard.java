package com.bingo.backend.card;

import com.bingo.backend.auth.User;
import com.bingo.backend.common.OrganizationCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bingo_cards")
public class BingoCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 4)
    private String serial;

    @Column(name = "numbers_json", nullable = false, columnDefinition = "longtext")
    private String numbersJson;

    @Column(name = "positional_signature", nullable = false, unique = true, length = 64)
    private String positionalSignature;

    @Column(name = "numbers_signature", nullable = false, length = 64)
    private String numbersSignature;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_code", nullable = false, length = 20)
    private OrganizationCode organizationCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status = CardStatus.AVAILABLE;

    @Column(name = "buyer_name", length = 160)
    private String buyerName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
