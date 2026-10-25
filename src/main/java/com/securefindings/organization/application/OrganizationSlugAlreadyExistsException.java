package com.securefindings.organization.application;

public class OrganizationSlugAlreadyExistsException extends RuntimeException {

    private final String slug;

    public OrganizationSlugAlreadyExistsException(String slug) {
        this(slug, null);
    }

    public OrganizationSlugAlreadyExistsException(String slug, Throwable cause) {
        super("Ya existe una organización con ese slug", cause);
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }
}