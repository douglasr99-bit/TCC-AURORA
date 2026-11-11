package br.com.aurora.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.dto.EstabelecimentoDto;
import br.com.aurora.service.EstabelecimentoService;
import jakarta.validation.Valid;

/** RF03. */
@RestController
@RequestMapping("/api/estabelecimento")
public class EstabelecimentoController {

    private final EstabelecimentoService servico;

    public EstabelecimentoController(EstabelecimentoService servico) {
        this.servico = servico;
    }

    /** 204 enquanto o estabelecimento ainda não foi cadastrado. */
    @GetMapping
    public ResponseEntity<EstabelecimentoDto> obter() {
        return servico.obter().map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }

    @PutMapping
    public EstabelecimentoDto salvar(@Valid @RequestBody EstabelecimentoDto dados) {
        return servico.salvar(dados);
    }
}
