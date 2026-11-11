package br.com.aurora.dto;

import br.com.aurora.validacao.Cns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfissionalEntrada(
        @NotBlank(message = "Informe o nome do profissional.")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "Informe o CNS do profissional.")
        @Pattern(regexp = "\\d{15}", message = "O CNS deve ter 15 dígitos, só números. Confira o cartão do profissional.")
        @Cns
        String cns,

        @NotBlank(message = "Informe o CBO (ocupação) do profissional.")
        @Pattern(regexp = "[0-9A-Z]{6}", message = "O CBO deve ter 6 caracteres (ex.: 225125). Confira no CNES.")
        String cbo,

        Boolean ativo) {
}
