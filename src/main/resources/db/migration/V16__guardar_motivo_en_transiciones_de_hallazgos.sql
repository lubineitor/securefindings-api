ALTER TABLE finding_audit
    ADD COLUMN reason VARCHAR(500);

ALTER TABLE finding_audit
    ADD CONSTRAINT ck_finding_audit_reason_for_status_change
    CHECK (
        reason IS NULL
        OR (
            action = 'STATUS_CHANGED'
            AND char_length(reason) BETWEEN 10 AND 500
            AND length(btrim(reason)) > 0
        )
    );