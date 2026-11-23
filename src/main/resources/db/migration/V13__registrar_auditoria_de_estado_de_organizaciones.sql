CREATE TABLE organization_status_audit (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_id VARCHAR(64),

    CONSTRAINT fk_organization_status_audit_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id),

    CONSTRAINT ck_organization_status_audit_action
        CHECK (action IN ('SUSPENDED', 'REACTIVATED')),

    CONSTRAINT ck_organization_status_audit_actor_not_blank
        CHECK (length(trim(actor)) > 0)
);

CREATE INDEX idx_organization_status_audit_org_time
    ON organization_status_audit (organization_id, occurred_at DESC, id DESC);