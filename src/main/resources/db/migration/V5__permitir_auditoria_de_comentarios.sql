DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'finding_audit'::regclass
          AND contype = 'c'
          AND pg_get_constraintdef(oid) ILIKE '%action%'
    LOOP
        EXECUTE format(
            'ALTER TABLE finding_audit DROP CONSTRAINT %I',
            constraint_record.conname
        );
    END LOOP;
END
$$;

ALTER TABLE finding_audit
    ADD CONSTRAINT ck_finding_audit_action
        CHECK (action IN (
            'CREATED',
            'UPDATED',
            'DELETED',
            'COMMENTED'
        ));