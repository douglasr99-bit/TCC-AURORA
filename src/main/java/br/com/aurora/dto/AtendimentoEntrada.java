package br.com.aurora.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

/**
 * Lançamento de atendimento (RF07). Paciente, idade, CID e caráter de atendimento dependem
 * do procedimento e são conferidos no serviço.
 */
public record AtendimentoEntrada(
        @NotNull(message = "Informe a data do atendimento.")
        LocalDate data,

        @NotNull(message = "Escolha o profissional que fez o atendimento.")
        Long profissionalId,

        @NotNull(message = "Escolha o procedimento realizado.")
        Long procedimentoId,

        @NotNull(message = "Informe a quantidade.")
        @Positive(message = "A quantidade deve ser maior que zero.")
        @Max(value = 999999, message = "A quantidade pode ter no máximo 6 dígitos (até 999999).")
        Integer quantidade,

        Long pacienteId,

        Integer idade,

        @Pattern(regexp = "[A-Z]\\d{2}\\d?",
                message = "O CID-10 deve ter uma letra e 2 ou 3 números, sem ponto (ex.: J069 ou I10).")
        String cid,

        String caraterAtendimento) {
}
