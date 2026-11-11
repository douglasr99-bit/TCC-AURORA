package br.com.aurora.dto;

import br.com.aurora.domain.Estabelecimento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados do cabeçalho do BPA. Tamanhos conforme o layout de exportação (DATASUS). */
public record EstabelecimentoDto(
        @NotBlank(message = "Informe o CNES do estabelecimento.")
        @Pattern(regexp = "\\d{7}", message = "O CNES deve ter 7 dígitos, só números.")
        String cnes,

        @NotBlank(message = "Informe o nome do estabelecimento.")
        @Size(max = 30, message = "O nome pode ter no máximo 30 caracteres (limite do BPA). Use uma forma abreviada.")
        String nome,

        @NotBlank(message = "Informe a sigla.")
        @Size(max = 6, message = "A sigla pode ter no máximo 6 caracteres.")
        String sigla,

        @NotBlank(message = "Informe o CNPJ.")
        @Pattern(regexp = "\\d{14}", message = "O CNPJ deve ter 14 dígitos, só números.")
        String cnpj,

        @NotBlank(message = "Informe o órgão de destino do arquivo (ex.: a Secretaria Municipal de Saúde).")
        @Size(max = 40, message = "O órgão de destino pode ter no máximo 40 caracteres (limite do BPA).")
        String orgaoDestino,

        @NotBlank(message = "Escolha se o destino é municipal ou estadual.")
        @Pattern(regexp = "[ME]", message = "Escolha M (municipal) ou E (estadual).")
        String indicadorDestino) {

    public static EstabelecimentoDto de(Estabelecimento e) {
        return new EstabelecimentoDto(e.getCnes(), e.getNome(), e.getSigla(), e.getCnpj(),
                e.getOrgaoDestino(), String.valueOf(e.getIndicadorDestino()));
    }
}
