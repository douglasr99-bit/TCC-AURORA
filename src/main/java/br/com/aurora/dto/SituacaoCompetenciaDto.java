package br.com.aurora.dto;

import br.com.aurora.domain.SituacaoCompetencia;

/** Situação de uma competência para a barra superior; {@code situacao} nulo = ainda sem lançamentos. */
public record SituacaoCompetenciaDto(String mesAno, SituacaoCompetencia situacao) {
}
