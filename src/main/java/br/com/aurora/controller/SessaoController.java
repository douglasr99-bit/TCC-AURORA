package br.com.aurora.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.config.UsuarioAutenticado;
import br.com.aurora.dto.UsuarioLogadoDto;

@RestController
@RequestMapping("/api")
public class SessaoController {

    /** Usuário logado e perfis, para montar o menu. */
    @GetMapping("/me")
    public UsuarioLogadoDto me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return new UsuarioLogadoDto(usuario.getId(), usuario.getNome(), usuario.getUsername(), usuario.getPerfis());
    }

    /** Garante que o cookie XSRF-TOKEN exista antes de um POST/PUT/DELETE. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().build();
    }
}
