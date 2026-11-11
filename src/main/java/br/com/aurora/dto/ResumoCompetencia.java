package br.com.aurora.dto;

import java.util.List;

/**
 * Conferência da produção (RF11). Totais em quantidade de procedimentos, como no wireframe:
 * atendimentos = BPA consolidado + BPA individualizado.
 */
public record ResumoCompetencia(
        CompetenciaDto competencia,
        long totalAtendimentos,
        long totalBpaC,
        long totalBpaI,
        long profissionais,
        List<ProducaoProcedimento> porProcedimento,
        List<ProducaoProfissional> porProfissional,
        List<ReaberturaDto> reaberturas) {
}
