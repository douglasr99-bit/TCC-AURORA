package br.com.aurora.domain;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Converte {@link YearMonth} para o formato AAAAMM da coluna competencia.mes_ano. */
@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    private static final DateTimeFormatter AAAAMM = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    public String convertToDatabaseColumn(YearMonth mesAno) {
        return mesAno == null ? null : mesAno.format(AAAAMM);
    }

    @Override
    public YearMonth convertToEntityAttribute(String valor) {
        return valor == null ? null : YearMonth.parse(valor.trim(), AAAAMM);
    }
}
