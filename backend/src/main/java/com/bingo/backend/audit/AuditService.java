package com.bingo.backend.audit;

import com.bingo.backend.auth.User;
import com.bingo.backend.card.BingoCard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditLogRepository;

    public void register(BingoCard card, User user, AuditAction action, String notes) {
        AuditLog log = new AuditLog();
        log.setCard(card);
        log.setUser(user);
        log.setAction(action);
        log.setNotes(notes);
        auditLogRepository.save(log);
    }
}
