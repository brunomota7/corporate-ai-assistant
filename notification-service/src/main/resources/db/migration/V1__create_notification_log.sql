CREATE TABLE tb_notification_log (
    id UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    recipient VARCHAR(180) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    status VARCHAR(20) CHECK(status IN ('PENDING', 'SENT', 'FAILED')) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    sent_at TIMESTAMP
);

CREATE INDEX idx_notification_logs_recipient ON tb_notification_log (recipient);
CREATE INDEX idx_notification_logs_status ON tb_notification_log (status);
CREATE INDEX idx_notification_logs_event_type ON tb_notification_log (event_type);