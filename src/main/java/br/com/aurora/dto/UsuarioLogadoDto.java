package br.com.aurora.dto;

import java.util.Set;

import br.com.aurora.domain.Perfil;

public record UsuarioLogadoDto(Long id, String nome, String login, Set<Perfil> perfis) {
}
