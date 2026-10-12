CREATE INDEX idx_finding_audit_finding_org_actor_occurred_at
    ON finding_audit (
        finding_id,
        organization_id,
        actor,
        occurred_at,
        id
    );