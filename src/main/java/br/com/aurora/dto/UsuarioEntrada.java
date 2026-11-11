package br.com.aurora.dto;

import java.util.Set;

import br.com.aurora.domain.Perfil;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Senha obrigatória só na criação; na edição, em branco mantém a atual. */
public record UsuarioEntrada(
        @NotBlank(message = "Informe o nome do usuário.")
        @Size(max = 100, message = "O nome pode ter no máximo 100 caracteres.")
        String nome,

        @NotBlank(message = "Informe o login.")
        @Pattern(regexp = "[A-Za-z0-9._-]{3,50}",
                message = "O login deve ter de 3 a 50 caracteres: letras sem acento, números, ponto, hífen ou sublinhado.")
        String login,

        @Size(min = 8, max = 72, message = "A senha deve ter de 8 a 72 caracteres.")
        String senha,

        @NotEmpty(message = "Marque pelo menos um perfil.")
        Set<Perfil> perfis,

        Boolean ativo) {
}
