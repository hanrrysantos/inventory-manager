CREATE TABLE tb_restock_notifications (
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID         NOT NULL,
    owner_id        BIGINT       NOT NULL,
    product_id      BIGINT       NOT NULL,
    occurred_at     TIMESTAMP    NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    failure_reason  TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_restock_notification_event_id UNIQUE (event_id),
    CONSTRAINT fk_restock_notification_owner
        FOREIGN KEY (owner_id) REFERENCES tb_users (id)
);

CREATE INDEX idx_restock_notifications_status ON tb_restock_notifications (status);
