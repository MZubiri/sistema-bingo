package com.bingo.backend.card;

import com.bingo.backend.card.CardDtos.VerifyResponse;
import com.bingo.backend.common.ApiException;
import com.bingo.backend.pdf.PdfService;
import com.bingo.backend.common.Role;
import com.bingo.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {
    private final BingoCardRepository bingoCardRepository;
    private final CardService cardService;
    private final PdfService pdfService;

    @GetMapping("/{serial}/verify")
    public VerifyResponse verify(@PathVariable String serial) {
        return cardService.verify(serial);
    }

    @GetMapping("/{serial}/pdf")
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String serial) {
        BingoCard card = bingoCardRepository.findBySerial(serial)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Carton no encontrado"));
        if (card.getStatus() == CardStatus.AVAILABLE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El carton aun no fue asignado");
        }
        if (principal.user().getRole() != Role.ADMIN) {
            boolean ownCard = card.getAssignedTo() != null && card.getAssignedTo().getId().equals(principal.user().getId());
            if (!ownCard) {
                throw new ApiException(HttpStatus.FORBIDDEN, "No tienes autorizacion para ver este PDF");
            }
        }
        byte[] bytes = pdfService.generate(card);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("carton-" + card.getSerial() + ".pdf")
                        .build()
                        .toString())
                .body(bytes);
    }
}
