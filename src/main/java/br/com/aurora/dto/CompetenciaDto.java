package br.com.aurora.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.SituacaoCompetencia;

public record CompetenciaDto(Long id, String mesAno, SituacaoCompetencia situacao, LocalDateTime dataFechamento) {

    public static CompetenciaDto de(Competencia c) {
        return new CompetenciaDto(c.getId(), c.getMesAno().format(DateTimeFormatter.ofPattern("yyyyMM")),
                c.getSituacao(), c.getDataFechamento());
    }
}
