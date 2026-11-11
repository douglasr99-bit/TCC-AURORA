package br.com.aurora.dto;

import java.time.LocalDate;

import br.com.aurora.domain.Sexo;
import br.com.aurora.validacao.Cns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Dados do paciente exigidos pelo BPA-I. Tamanhos conforme o layout de exportação. */
public record PacienteEntrada(
        @NotBlank(message = "Informe o CNS do paciente.")
        @Pattern(regexp = "\\d{15}", message = "O CNS deve ter 15 dígitos, só números. Confira o cartão do paciente.")
        @Cns
        String cns,

        @NotBlank(message = "Informe o nome do paciente.")
        @Size(max = 30, message = "O nome pode ter no máximo 30 caracteres (limite do BPA). Abrevie os sobrenomes do meio.")
        String nome,

        @NotNull(message = "Informe a data de nascimento.")
        LocalDate dataNascimento,

        @NotNull(message = "Escolha o sexo do paciente.")
        Sexo sexo,

        @NotBlank(message = "Escolha a raça/cor do paciente.")
        String racaCor,

        String etnia,

        @NotBlank(message = "Escolha a nacionalidade do paciente.")
        String nacionalidade,

        @NotBlank(message = "Informe o código IBGE do município de residência.")
        @Pattern(regexp = "\\d{6}", message = "O código IBGE do município deve ter 6 dígitos, só números.")
        String codigoMunicipio,

        @Pattern(regexp = "\\d{8}", message = "O CEP deve ter 8 dígitos, só números.")
        String cep,

        @Size(max = 30, message = "O endereço pode ter no máximo 30 caracteres (limite do BPA). Abrevie (ex.: R., Av.).")
        String logradouro,

        @Size(max = 5, message = "O número pode ter no máximo 5 caracteres. Sem número, deixe em branco.")
        String numero,

        @Size(max = 30, message = "O bairro pode ter no máximo 30 caracteres (limite do BPA). Abrevie se precisar.")
        String bairro,

        @Pattern(regexp = "\\d{10,11}", message = "O telefone deve ter DDD e número: 10 ou 11 dígitos, só números.")
        String telefone) {
}
