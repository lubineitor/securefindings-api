package com.securefindings.asset.application;

public class AssetIdentifierAlreadyExistsException extends RuntimeException {

    private final String identifier;

    public AssetIdentifierAlreadyExistsException(String identifier) {
        this(identifier, null);
    }

    public AssetIdentifierAlreadyExistsException(String identifier, Throwable cause) {
        super("Ya existe un activo con ese identificador en la organización", cause);
        this.identifier = identifier;
    }

    public String identifier() {
        return identifier;
    }
}