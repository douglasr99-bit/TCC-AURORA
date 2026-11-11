package br.com.aurora.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import br.com.aurora.domain.Atendimento;
import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.SituacaoCompetencia;

public record AtendimentoSaida(
        Long id,
        LocalDate data,
        String competencia,
        SituacaoCompetencia situacaoCompetencia,
        Long profissionalId,
        String profissionalNome,
        String profissionalCbo,
        Long procedimentoId,
        String procedimentoCodigo,
        String procedimentoDescricao,
        Modalidade modalidade,
        boolean exigeCid,
        Long pacienteId,
        String pacienteNome,
        String pacienteCns,
        LocalDate pacienteDataNascimento,
        int quantidade,
        Integer idade,
        String cid,
        String caraterAtendimento,
        String registradoPor) {

    private static final DateTimeFormatter AAAAMM = DateTimeFormatter.ofPattern("yyyyMM");

    public static AtendimentoSaida de(Atendimento a) {
        Paciente p = a.getPaciente();
        return new AtendimentoSaida(a.getId(), a.getData(),
                a.getCompetencia() == null ? null : a.getCompetencia().getMesAno().format(AAAAMM),
                a.getCompetencia() == null ? null : a.getCompetencia().getSituacao(),
                a.getProfissional().getId(), a.getProfissional().getNome(), a.getProfissional().getCbo(),
                a.getProcedimento().getId(), a.getProcedimento().getCodigo(), a.getProcedimento().getDescricao(),
                a.getProcedimento().getModalidade(), a.getProcedimento().isExigeCid(),
                p == null ? null : p.getId(), p == null ? null : p.getNome(), p == null ? null : p.getCns(),
                p == null ? null : p.getDataNascimento(),
                a.getQuantidade(), a.getIdade(), a.getCid(), a.getCaraterAtendimento(),
                a.getUsuario().getNome());
    }
}
