package br.com.aurora.dto;

import br.com.aurora.domain.Modalidade;

public record ProducaoProcedimento(String codigo, String descricao, Modalidade modalidade, long lancamentos,
        long quantidade) {
}
