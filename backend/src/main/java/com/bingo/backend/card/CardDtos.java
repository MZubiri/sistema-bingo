package com.bingo.backend.card;

import com.bingo.backend.common.OrganizationCode;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.time.Instant;

public final class CardDtos {
    private CardDtos() {
    }

    public record GenerateCardRequest(
            @NotBlank(message = "El nombre del comprador es obligatorio") String buyerName,
            @NotBlank(message = "El serial del carton es obligatorio") String serial,
            String idempotencyKey
    ) {
    }

    public record AvailableCardResponse(
            String serial,
            OrganizationCode organizationCode,
            CardStatus status
    ) {
        public static AvailableCardResponse from(BingoCard card) {
            return new AvailableCardResponse(card.getSerial(), card.getOrganizationCode(), card.getStatus());
        }
    }

    public record PageResponse<T>(
            List<T> content,
            long totalElements,
            int totalPages,
            int page,
            int size
    ) {
    }

    public record CardResponse(
            Long id,
            String serial,
            String buyerName,
            String sellerName,
            OrganizationCode organizationCode,
            CardStatus status,
            String numbersJson,
            Instant assignedAt
    ) {
        public static CardResponse from(BingoCard card) {
            String seller = card.getAssignedTo() == null ? null : card.getAssignedTo().getFullName();
            return new CardResponse(
                    card.getId(),
                    card.getSerial(),
                    card.getBuyerName(),
                    seller,
                    card.getOrganizationCode(),
                    card.getStatus(),
                    card.getNumbersJson(),
                    card.getAssignedAt()
            );
        }
    }

    public record VerifyResponse(
            String serial,
            CardStatus status,
            String buyerName,
            String sellerName,
            OrganizationCode organizationCode,
            Instant assignedAt,
            String numbersJson
    ) {
        public static VerifyResponse from(BingoCard card) {
            String seller = card.getAssignedTo() == null ? null : card.getAssignedTo().getFullName();
            return new VerifyResponse(
                    card.getSerial(),
                    card.getStatus(),
                    card.getBuyerName(),
                    seller,
                    card.getOrganizationCode(),
                    card.getAssignedAt(),
                    card.getNumbersJson()
            );
        }
    }
}
