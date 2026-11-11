package br.com.aurora.validacao;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * CNS com dígito verificador válido. Só confere a regra quando o valor já tem 15 dígitos;
 * o formato é verificado por {@code @Pattern}, com mensagem própria.
 */
@Documented
@Constraint(validatedBy = CnsValidator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Cns {

    String message() default "Este número de CNS não é válido. Confira os 15 dígitos no cartão.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
