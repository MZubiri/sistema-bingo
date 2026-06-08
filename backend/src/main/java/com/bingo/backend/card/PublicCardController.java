package com.bingo.backend.card;

import com.bingo.backend.card.CardDtos.VerifyResponse;
import com.bingo.backend.common.ApiException;
import com.bingo.backend.pdf.PdfService;
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

@RestController
@RequestMapping("/api/public/cards")
@RequiredArgsConstructor
public class PublicCardController {
    private final CardService cardService;
    private final BingoCardRepository bingoCardRepository;
    private final PdfService pdfService;

    @GetMapping("/{serial}/verify")
    public VerifyResponse verify(@PathVariable String serial) {
        return cardService.verify(serial);
    }

    @GetMapping("/{serial}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String serial) {
        BingoCard card = bingoCardRepository.findBySerial(serial)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Cartón no encontrado"));
        if (card.getStatus() != CardStatus.ASSIGNED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El cartón no está activo para descarga");
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
