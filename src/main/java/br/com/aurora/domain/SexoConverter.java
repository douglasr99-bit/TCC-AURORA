package br.com.aurora.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Grava o {@link Sexo} na coluna CHAR(1). */
@Converter(autoApply = true)
public class SexoConverter implements AttributeConverter<Sexo, Character> {

    @Override
    public Character convertToDatabaseColumn(Sexo sexo) {
        return sexo == null ? null : sexo.name().charAt(0);
    }

    @Override
    public Sexo convertToEntityAttribute(Character valor) {
        return valor == null ? null : Sexo.valueOf(String.valueOf(valor));
    }
}
