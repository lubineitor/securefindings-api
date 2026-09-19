ALTER TABLE finding_audit
    ADD COLUMN request_id VARCHAR(64);

CREATE INDEX idx_finding_audit_request_id
    ON finding_audit (request_id)
    WHERE request_id IS NOT NULL;