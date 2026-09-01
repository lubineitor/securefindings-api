package com.securefindings.finding.application;

import java.util.UUID;

public class FindingNotFoundException extends RuntimeException {

    public FindingNotFoundException(UUID id) {
        super("No se ha encontrado el hallazgo con identificador: " + id);
    }
}