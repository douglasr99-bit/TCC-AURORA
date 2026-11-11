package br.com.aurora.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.config.UsuarioAutenticado;
import br.com.aurora.dto.UsuarioEntrada;
import br.com.aurora.dto.UsuarioSaida;
import br.com.aurora.service.UsuarioService;
import jakarta.validation.Valid;

/** RF02. */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService servico;

    public UsuarioController(UsuarioService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<UsuarioSaida> listar() {
        return servico.listar();
    }

    @GetMapping("/{id}")
    public UsuarioSaida obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioSaida criar(@Valid @RequestBody UsuarioEntrada dados) {
        return servico.criar(dados);
    }

    @PutMapping("/{id}")
    public UsuarioSaida atualizar(@PathVariable Long id, @Valid @RequestBody UsuarioEntrada dados,
            @AuthenticationPrincipal UsuarioAutenticado logado) {
        return servico.atualizar(id, dados, logado.getId());
    }
}
