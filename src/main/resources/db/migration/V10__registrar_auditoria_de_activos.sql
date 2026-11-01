CREATE TABLE asset_audit (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    action VARCHAR(20) NOT NULL,
    actor VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_id VARCHAR(64),

    CONSTRAINT fk_asset_audit_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id),

    CONSTRAINT ck_asset_audit_action
        CHECK (action IN ('CREATED', 'UPDATED', 'ARCHIVED', 'RESTORED', 'DELETED')),

    CONSTRAINT ck_asset_audit_actor_not_blank
        CHECK (length(trim(actor)) > 0)
);

CREATE INDEX idx_asset_audit_organization_asset_time
    ON asset_audit (organization_id, asset_id, occurred_at, id);