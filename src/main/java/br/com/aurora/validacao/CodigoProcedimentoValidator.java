package br.com.aurora.validacao;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CodigoProcedimentoValidator implements ConstraintValidator<CodigoProcedimento, String> {

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext ctx) {
        if (valor == null || !valor.matches("\\d{10}")) {
            return true;
        }
        return ValidadorProcedimento.valido(valor);
    }
}
