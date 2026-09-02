CREATE TABLE finding_audit (
    id UUID PRIMARY KEY,
    finding_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT finding_audit_action_check
        CHECK (action IN ('CREATED', 'UPDATED', 'DELETED'))
);

CREATE INDEX idx_finding_audit_finding_id
    ON finding_audit (finding_id);

CREATE INDEX idx_finding_audit_occurred_at
    ON finding_audit (occurred_at);