package com.bingo.backend.admin;

import com.bingo.backend.admin.AdminDtos.AdminCardResponse;
import com.bingo.backend.admin.AdminDtos.DashboardResponse;
import com.bingo.backend.admin.AdminDtos.OrganizationStats;
import com.bingo.backend.admin.AdminDtos.SellerStats;
import com.bingo.backend.auth.UserRepository;
import com.bingo.backend.card.BingoCard;
import com.bingo.backend.card.BingoCardRepository;
import com.bingo.backend.card.CardService;
import com.bingo.backend.card.CardStatus;
import com.bingo.backend.common.OrganizationCode;
import com.bingo.backend.common.Role;
import com.bingo.backend.security.UserPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final BingoCardRepository bingoCardRepository;
    private final UserRepository userRepository;
    private final CardService cardService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        long assigned = bingoCardRepository.countByStatus(CardStatus.ASSIGNED);
        long available = bingoCardRepository.countByStatus(CardStatus.AVAILABLE);
        long cancelled = bingoCardRepository.countByStatus(CardStatus.CANCELLED);
        List<OrganizationStats> organizations = Arrays.stream(OrganizationCode.values())
                .filter(code -> code != OrganizationCode.ADMIN)
                .map(code -> new OrganizationStats(
                        code,
                        bingoCardRepository.countByOrganizationCode(code),
                        bingoCardRepository.countByOrganizationCodeAndStatus(code, CardStatus.ASSIGNED),
                        bingoCardRepository.countByOrganizationCodeAndStatus(code, CardStatus.AVAILABLE),
                        bingoCardRepository.countByOrganizationCodeAndStatus(code, CardStatus.CANCELLED)
                ))
                .toList();
        List<SellerStats> sellers = userRepository.findByRole(Role.VENDEDOR).stream()
                .map(user -> new SellerStats(
                        user.getId(),
                        user.getUsername(),
                        user.getFullName(),
                        user.getOrganizationCode(),
                        bingoCardRepository.findByAssignedToIdOrderByAssignedAtDesc(user.getId()).size()
                ))
                .toList();
        return new DashboardResponse(assigned + available + cancelled, assigned, available, cancelled, organizations, sellers);
    }

    @GetMapping("/cards")
    public List<AdminCardResponse> cards(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) OrganizationCode organizationCode,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate assignedTo
    ) {
        String cleanSearch = search == null || search.isBlank() ? null : search.trim();
        Instant from = assignedFrom == null ? null : assignedFrom.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant to = assignedTo == null ? null : assignedTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return bingoCardRepository.searchAdminCards(cleanSearch, organizationCode, status, from, to).stream()
                .map(this::toAdminCard)
                .toList();
    }

    @PatchMapping("/cards/{id}/cancel")
    public Object cancel(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return cardService.cancel(id, principal.user());
    }

    private AdminCardResponse toAdminCard(BingoCard card) {
        String sellerName = card.getAssignedTo() == null ? null : card.getAssignedTo().getFullName();
        return new AdminCardResponse(
                card.getId(),
                card.getSerial(),
                card.getOrganizationCode(),
                card.getStatus(),
                card.getBuyerName(),
                sellerName,
                card.getAssignedAt(),
                card.getCancelledAt()
        );
    }
}
