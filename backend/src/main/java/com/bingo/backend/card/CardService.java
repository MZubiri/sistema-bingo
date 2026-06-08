package com.bingo.backend.card;

import com.bingo.backend.audit.AuditAction;
import com.bingo.backend.audit.AuditService;
import com.bingo.backend.auth.User;
import com.bingo.backend.auth.UserRepository;
import com.bingo.backend.card.CardDtos.CardResponse;
import com.bingo.backend.card.CardDtos.GenerateCardRequest;
import com.bingo.backend.card.CardDtos.AvailableCardResponse;
import com.bingo.backend.card.CardDtos.PageResponse;
import com.bingo.backend.card.CardDtos.VerifyResponse;
import com.bingo.backend.common.ApiException;
import com.bingo.backend.common.HashService;
import com.bingo.backend.common.Role;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CardService {
    private final BingoCardRepository bingoCardRepository;
    private final IdempotencyRequestRepository idempotencyRequestRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final HashService hashService;

    @Transactional
    public CardResponse generate(User principalUser, GenerateCardRequest request, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey) && request != null) {
            idempotencyKey = request.idempotencyKey();
        }
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La clave de idempotencia es obligatoria");
        }
        String buyerName = cleanBuyerName(request == null ? null : request.buyerName());
        String serial = cleanSerial(request == null ? null : request.serial());
        String requestHash = hashService.sha256(buyerName + "|" + serial);

        User user = userRepository.findByIdForUpdate(principalUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
        if (user.getRole() != Role.VENDEDOR || !user.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Representante no autorizado");
        }

        IdempotencyRequest existing = idempotencyRequestRepository
                .findByUserAndKeyForUpdate(user.getId(), idempotencyKey)
                .orElse(null);
        if (existing != null) {
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "La clave de idempotencia ya fue usada con otra solicitud");
            }
            if (existing.getCard() == null) {
                throw new ApiException(HttpStatus.CONFLICT, "La solicitud previa aun esta en proceso");
            }
            return CardResponse.from(existing.getCard());
        }

        IdempotencyRequest idempotencyRequest = new IdempotencyRequest();
        idempotencyRequest.setUser(user);
        idempotencyRequest.setIdempotencyKey(idempotencyKey);
        idempotencyRequest.setRequestHash(requestHash);
        try {
            idempotencyRequest = idempotencyRequestRepository.saveAndFlush(idempotencyRequest);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "Solicitud duplicada en proceso, intenta consultar nuevamente");
        }

        BingoCard card = bingoCardRepository.findBySerialForUpdate(serial)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El carton seleccionado no existe"));
        if (card.getOrganizationCode() != user.getOrganizationCode()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "No puedes generar cartones de otra agrupacion.");
        }
        if (card.getStatus() == CardStatus.ASSIGNED) {
            throw new ApiException(HttpStatus.CONFLICT, "Este carton ya no esta disponible. Selecciona otro.");
        }
        if (card.getStatus() == CardStatus.CANCELLED) {
            throw new ApiException(HttpStatus.CONFLICT, "Este carton fue anulado y no puede generarse.");
        }

        card.setStatus(CardStatus.ASSIGNED);
        card.setBuyerName(buyerName);
        card.setAssignedTo(user);
        card.setAssignedAt(Instant.now());
        bingoCardRepository.save(card);

        idempotencyRequest.setCard(card);
        idempotencyRequestRepository.save(idempotencyRequest);

        auditService.register(card, user, AuditAction.CARD_ASSIGNED, "Carton asignado a " + buyerName);
        return CardResponse.from(card);
    }

    @Transactional(readOnly = true)
    public PageResponse<AvailableCardResponse> available(User user, String search, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Page<AvailableCardResponse> result = bingoCardRepository
                .searchAvailable(user.getOrganizationCode(), CardStatus.AVAILABLE, cleanSearch, pageable)
                .map(AvailableCardResponse::from);
        return new PageResponse<>(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public List<CardResponse> history(User user, String search, CardStatus status, Instant assignedFrom, Instant assignedTo) {
        String cleanSearch = StringUtils.hasText(search) ? search.trim() : null;
        return bingoCardRepository.searchHistory(user.getId(), cleanSearch, status, assignedFrom, assignedTo)
                .stream()
                .map(CardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VerifyResponse verify(String serial) {
        BingoCard card = bingoCardRepository.findBySerial(serial)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Carton no encontrado"));
        return VerifyResponse.from(card);
    }

    @Transactional
    public CardResponse cancel(Long cardId, User admin) {
        BingoCard card = bingoCardRepository.findById(cardId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Carton no encontrado"));
        if (card.getStatus() != CardStatus.ASSIGNED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Solo se pueden cancelar cartones asignados");
        }
        card.setStatus(CardStatus.CANCELLED);
        card.setCancelledAt(Instant.now());
        bingoCardRepository.save(card);
        auditService.register(card, admin, AuditAction.CARD_CANCELLED, "Carton cancelado por administrador");
        return CardResponse.from(card);
    }

    private String cleanBuyerName(String buyerName) {
        if (!StringUtils.hasText(buyerName)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El nombre del comprador es obligatorio");
        }
        String clean = buyerName.trim().replaceAll("\\s+", " ");
        if (clean.length() < 3 || clean.length() > 160) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El nombre del comprador debe tener entre 3 y 160 caracteres");
        }
        return clean;
    }

    private String cleanSerial(String serial) {
        if (!StringUtils.hasText(serial)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El serial del carton es obligatorio");
        }
        String clean = serial.trim();
        if (!clean.matches("\\d{4}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El serial debe tener 4 digitos");
        }
        return clean;
    }
}
