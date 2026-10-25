CREATE TABLE assets (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    identifier VARCHAR(255) NOT NULL,
    asset_type VARCHAR(30) NOT NULL,
    environment VARCHAR(30) NOT NULL,
    criticality VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_assets_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id),
    CONSTRAINT uk_assets_id_organization
        UNIQUE (id, organization_id),
    CONSTRAINT ck_assets_name_not_blank
        CHECK (length(trim(name)) > 0),
    CONSTRAINT ck_assets_identifier_not_blank
        CHECK (length(trim(identifier)) > 0)
);

CREATE UNIQUE INDEX uk_assets_organization_identifier
    ON assets (organization_id, lower(identifier));

CREATE INDEX idx_assets_organization_name_id
    ON assets (organization_id, name, id);

ALTER TABLE findings
    ADD COLUMN asset_id UUID;

ALTER TABLE findings
    ADD CONSTRAINT fk_findings_asset_organization
        FOREIGN KEY (asset_id, organization_id)
        REFERENCES assets (id, organization_id);

CREATE INDEX idx_findings_organization_asset_created
    ON findings (organization_id, asset_id, created_at, id);