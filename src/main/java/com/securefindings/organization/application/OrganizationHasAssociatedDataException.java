package com.securefindings.organization.application;

import java.util.UUID;

public class OrganizationHasAssociatedDataException extends RuntimeException {

    private final UUID organizationId;

    public OrganizationHasAssociatedDataException(UUID organizationId) {
        this(organizationId, null);
    }

    public OrganizationHasAssociatedDataException(
            UUID organizationId,
            Throwable cause) {
        super(
                "No se puede eliminar una organización que conserva datos asociados",
                cause);
        this.organizationId = organizationId;
    }

    public UUID organizationId() {
        return organizationId;
    }
}