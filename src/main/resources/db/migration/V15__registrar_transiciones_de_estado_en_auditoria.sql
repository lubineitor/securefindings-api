ALTER TABLE finding_audit
    ADD COLUMN previous_status VARCHAR(20),
    ADD COLUMN new_status VARCHAR(20);

ALTER TABLE finding_audit
    DROP CONSTRAINT ck_finding_audit_action;

ALTER TABLE finding_audit
    ADD CONSTRAINT ck_finding_audit_action
        CHECK (action IN (
            'CREATED',
            'UPDATED',
            'STATUS_CHANGED',
            'DELETED',
            'COMMENTED'
        ));

ALTER TABLE finding_audit
    ADD CONSTRAINT ck_finding_audit_status_change
        CHECK (
            (
                action = 'STATUS_CHANGED'
                AND previous_status IS NOT NULL
                AND new_status IS NOT NULL
                AND previous_status <> new_status
            )
            OR (
                action <> 'STATUS_CHANGED'
                AND previous_status IS NULL
                AND new_status IS NULL
            )
        );

ALTER TABLE finding_audit
    ADD CONSTRAINT ck_finding_audit_status_values
        CHECK (
            (previous_status IS NULL OR previous_status IN (
                'OPEN',
                'IN_PROGRESS',
                'RESOLVED',
                'FALSE_POSITIVE'
            ))
            AND
            (new_status IS NULL OR new_status IN (
                'OPEN',
                'IN_PROGRESS',
                'RESOLVED',
                'FALSE_POSITIVE'
            ))
        );