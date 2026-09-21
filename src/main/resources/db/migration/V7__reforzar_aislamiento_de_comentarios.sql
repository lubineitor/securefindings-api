ALTER TABLE finding_comments
    DROP CONSTRAINT fk_finding_comments_finding;

ALTER TABLE findings
    ADD CONSTRAINT uk_findings_id_organization
        UNIQUE (id, organization_id);

ALTER TABLE finding_comments
    ADD CONSTRAINT fk_finding_comments_finding_organization
        FOREIGN KEY (finding_id, organization_id)
        REFERENCES findings (id, organization_id)
        ON DELETE CASCADE;