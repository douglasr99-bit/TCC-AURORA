package br.com.aurora.dto;

import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.Procedimento;

public record ProcedimentoSaida(Long id, String codigo, String descricao, Modalidade modalidade, boolean exigeCid) {

    public static ProcedimentoSaida de(Procedimento p) {
        return new ProcedimentoSaida(p.getId(), p.getCodigo(), p.getDescricao(), p.getModalidade(), p.isExigeCid());
    }
}
