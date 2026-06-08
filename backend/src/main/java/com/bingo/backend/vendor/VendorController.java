package com.bingo.backend.vendor;

import com.bingo.backend.card.BingoCardRepository;
import com.bingo.backend.card.CardDtos.AvailableCardResponse;
import com.bingo.backend.card.CardDtos.CardResponse;
import com.bingo.backend.card.CardDtos.GenerateCardRequest;
import com.bingo.backend.card.CardDtos.PageResponse;
import com.bingo.backend.card.CardService;
import com.bingo.backend.card.CardStatus;
import com.bingo.backend.security.UserPrincipal;
import com.bingo.backend.vendor.VendorDtos.VendorDashboardResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/vendor", "/api/representative"})
@RequiredArgsConstructor
public class VendorController {
    private final CardService cardService;
    private final BingoCardRepository bingoCardRepository;

    @GetMapping("/dashboard")
    public VendorDashboardResponse dashboard(@AuthenticationPrincipal UserPrincipal principal) {
        var user = principal.user();
        long total = bingoCardRepository.countByOrganizationCode(user.getOrganizationCode());
        long assigned = bingoCardRepository.countByOrganizationCodeAndStatus(user.getOrganizationCode(), CardStatus.ASSIGNED);
        long available = bingoCardRepository.countByOrganizationCodeAndStatus(user.getOrganizationCode(), CardStatus.AVAILABLE);
        return new VendorDashboardResponse(
                user.getFullName(),
                user.getOrganizationCode(),
                total,
                assigned,
                available
        );
    }

    @GetMapping("/cards/available")
    public PageResponse<AvailableCardResponse> available(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return cardService.available(principal.user(), search, page, size);
    }

    @PostMapping("/cards/generate")
    public CardResponse generate(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody GenerateCardRequest request
    ) {
        return cardService.generate(principal.user(), request, idempotencyKey);
    }

    @GetMapping("/cards")
    public List<CardResponse> cards(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignedTo
    ) {
        Instant from = assignedFrom == null ? null : assignedFrom.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = assignedTo == null ? null : assignedTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return cardService.history(principal.user(), search, status, from, to);
    }
}
