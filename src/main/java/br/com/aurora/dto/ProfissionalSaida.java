package br.com.aurora.dto;

import br.com.aurora.domain.Profissional;

public record ProfissionalSaida(Long id, String nome, String cns, String cbo, boolean ativo) {

    public static ProfissionalSaida de(Profissional p) {
        return new ProfissionalSaida(p.getId(), p.getNome(), p.getCns(), p.getCbo(), p.isAtivo());
    }
}
