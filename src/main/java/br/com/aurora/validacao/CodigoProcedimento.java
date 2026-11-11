package br.com.aurora.validacao;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/** Código de procedimento com dígito verificador válido (só confere quando já tem 10 dígitos). */
@Documented
@Constraint(validatedBy = CodigoProcedimentoValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface CodigoProcedimento {

    String message() default "Código de procedimento inválido: o último dígito não confere. "
            + "Confira o código na Tabela SUS (SIGTAP).";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
