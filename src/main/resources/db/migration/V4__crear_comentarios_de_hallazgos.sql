CREATE TABLE finding_comments (
    id UUID PRIMARY KEY,
    finding_id UUID NOT NULL,
    organization_id UUID NOT NULL,
    author VARCHAR(255) NOT NULL,
    content VARCHAR(5000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_finding_comments_finding
        FOREIGN KEY (finding_id)
        REFERENCES findings (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_finding_comments_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations (id),

    CONSTRAINT ck_finding_comments_content
        CHECK (length(trim(content)) > 0)
);

CREATE INDEX idx_finding_comments_finding_organization_created
    ON finding_comments (
        finding_id,
        organization_id,
        created_at,
        id
    );