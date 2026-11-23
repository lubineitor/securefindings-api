package com.securefindings.organization.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CodePointSizeValidator
        implements ConstraintValidator<CodePointSize, CharSequence> {

    private int min;
    private int max;

    @Override
    public void initialize(CodePointSize constraintAnnotation) {
        min = constraintAnnotation.min();
        max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(
            CharSequence value,
            ConstraintValidatorContext context) {

        // @NotBlank se encarga de rechazar null y cadenas vacías.
        if (value == null) {
            return true;
        }

        String text = value.toString();
        int codePointCount = text.codePointCount(0, text.length());

        return codePointCount >= min && codePointCount <= max;
    }
}