package com.bingo.backend.admin;

import com.bingo.backend.card.CardStatus;
import com.bingo.backend.common.OrganizationCode;
import java.time.Instant;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {
    }

    public record DashboardResponse(
            long totalCards,
            long assignedCards,
            long availableCards,
            long cancelledCards,
            List<OrganizationStats> organizations,
            List<SellerStats> sellers
    ) {
    }

    public record OrganizationStats(
            OrganizationCode organizationCode,
            long total,
            long assigned,
            long available,
            long cancelled
    ) {
    }

    public record SellerStats(
            Long userId,
            String username,
            String fullName,
            OrganizationCode organizationCode,
            long soldCards
    ) {
    }

    public record AdminCardResponse(
            Long id,
            String serial,
            OrganizationCode organizationCode,
            CardStatus status,
            String buyerName,
            String sellerName,
            Instant assignedAt,
            Instant cancelledAt
    ) {
    }
}
