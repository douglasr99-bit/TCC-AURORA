package br.com.aurora.dto;

import java.util.Set;

import br.com.aurora.domain.Perfil;
import br.com.aurora.domain.Usuario;

public record UsuarioSaida(Long id, String nome, String login, Set<Perfil> perfis, boolean ativo) {

    public static UsuarioSaida de(Usuario u) {
        return new UsuarioSaida(u.getId(), u.getNome(), u.getLogin(), Set.copyOf(u.getPerfis()), u.isAtivo());
    }
}
