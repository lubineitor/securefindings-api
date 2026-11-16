ALTER TABLE findings
    ADD COLUMN due_at TIMESTAMPTZ;

CREATE INDEX idx_findings_organization_status_due_at
    ON findings (organization_id, status, due_at)
    WHERE due_at IS NOT NULL;