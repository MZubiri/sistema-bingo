CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(80) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    role VARCHAR(20) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    quota_total INT NOT NULL DEFAULT 0,
    quota_used INT NOT NULL DEFAULT 0,
    organization_code VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'VENDEDOR')),
    CONSTRAINT chk_users_organization CHECK (organization_code IN ('GEOURP', 'CIVIAL', 'ACI', 'ADMIN')),
    CONSTRAINT chk_users_quota CHECK (quota_used <= quota_total)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bingo_cards (
    id BIGINT NOT NULL AUTO_INCREMENT,
    serial VARCHAR(4) NOT NULL,
    numbers_json LONGTEXT NOT NULL,
    positional_signature VARCHAR(64) NOT NULL,
    numbers_signature VARCHAR(64) NOT NULL,
    organization_code VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    buyer_name VARCHAR(160) NULL,
    assigned_to_user_id BIGINT NULL,
    assigned_at TIMESTAMP(6) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_bingo_cards_serial UNIQUE (serial),
    CONSTRAINT uk_bingo_cards_positional_signature UNIQUE (positional_signature),
    CONSTRAINT fk_bingo_cards_assigned_user FOREIGN KEY (assigned_to_user_id) REFERENCES users (id),
    CONSTRAINT chk_bingo_cards_status CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'CANCELLED')),
    CONSTRAINT chk_bingo_cards_organization CHECK (organization_code IN ('GEOURP', 'CIVIAL', 'ACI')),
    CONSTRAINT chk_bingo_cards_buyer CHECK (
        (status = 'AVAILABLE' AND buyer_name IS NULL)
        OR (status IN ('ASSIGNED', 'CANCELLED') AND buyer_name IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_bingo_cards_assignment ON bingo_cards (organization_code, status, serial);
CREATE INDEX idx_bingo_cards_assigned_user ON bingo_cards (assigned_to_user_id, assigned_at);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    card_id BIGINT NULL,
    user_id BIGINT NULL,
    action VARCHAR(40) NOT NULL,
    notes VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_audit_logs_card FOREIGN KEY (card_id) REFERENCES bingo_cards (id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_logs_card ON audit_logs (card_id, created_at);
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id, created_at);

CREATE TABLE idempotency_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(120) NOT NULL,
    user_id BIGINT NOT NULL,
    card_id BIGINT NULL,
    request_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, idempotency_key),
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_idempotency_card FOREIGN KEY (card_id) REFERENCES bingo_cards (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
