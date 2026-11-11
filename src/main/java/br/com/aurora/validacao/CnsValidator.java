package br.com.aurora.validacao;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CnsValidator implements ConstraintValidator<Cns, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        if (valor == null || !valor.matches("\\d{15}")) {
            return true;
        }
        return ValidadorCns.valido(valor);
    }
}
