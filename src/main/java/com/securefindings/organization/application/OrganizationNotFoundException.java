package com.securefindings.organization.application;

import java.util.UUID;

public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(UUID organizationId) {
        super("No se ha encontrado la organización " + organizationId);
    }
}