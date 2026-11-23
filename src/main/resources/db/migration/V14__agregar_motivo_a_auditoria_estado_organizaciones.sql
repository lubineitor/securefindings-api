ALTER TABLE organization_status_audit
    ADD COLUMN reason VARCHAR(500);

ALTER TABLE organization_status_audit
    ADD CONSTRAINT ck_organization_status_audit_reason_min_length
    CHECK (reason IS NULL OR length(trim(reason)) >= 10);