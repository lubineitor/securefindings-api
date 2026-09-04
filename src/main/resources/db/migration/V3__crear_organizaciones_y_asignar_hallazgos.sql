CREATE TABLE organizations (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    slug VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT uk_organizations_slug
        UNIQUE (slug)
);

INSERT INTO organizations (
    id,
    name,
    slug,
    created_at
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Organización de desarrollo',
    'desarrollo',
    CURRENT_TIMESTAMP
);

ALTER TABLE findings
    ADD COLUMN organization_id UUID NOT NULL
        DEFAULT '00000000-0000-0000-0000-000000000001';

ALTER TABLE findings
    ADD CONSTRAINT fk_findings_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id);

CREATE INDEX idx_findings_organization_id
    ON findings (organization_id);

ALTER TABLE finding_audit
    ADD COLUMN organization_id UUID NOT NULL
        DEFAULT '00000000-0000-0000-0000-000000000001';

ALTER TABLE finding_audit
    ADD CONSTRAINT fk_finding_audit_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id);

CREATE INDEX idx_finding_audit_organization_id
    ON finding_audit (organization_id);