package br.com.aurora.dto;

import br.com.aurora.domain.Modalidade;
import br.com.aurora.validacao.CodigoProcedimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProcedimentoEntrada(
        @NotBlank(message = "Informe o código do procedimento.")
        @Pattern(regexp = "\\d{10}", message = "O código do procedimento deve ter 10 dígitos, só números (ex.: 0301010072).")
        @CodigoProcedimento
        String codigo,

        @NotBlank(message = "Informe a descrição do procedimento.")
        @Size(max = 250, message = "A descrição pode ter no máximo 250 caracteres.")
        String descricao,

        @NotNull(message = "Escolha o instrumento de registro: BPA-C (consolidado) ou BPA-I (individualizado).")
        Modalidade modalidade,

        Boolean exigeCid) {
}
