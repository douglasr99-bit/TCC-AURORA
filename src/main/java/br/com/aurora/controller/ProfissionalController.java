package br.com.aurora.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.dto.ProfissionalEntrada;
import br.com.aurora.dto.ProfissionalSaida;
import br.com.aurora.service.ProfissionalService;
import jakarta.validation.Valid;

/** RF04. */
@RestController
@RequestMapping("/api/profissionais")
public class ProfissionalController {

    private final ProfissionalService servico;

    public ProfissionalController(ProfissionalService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ProfissionalSaida> buscar(@RequestParam(required = false) String q) {
        return servico.buscar(q);
    }

    @GetMapping("/{id}")
    public ProfissionalSaida obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfissionalSaida criar(@Valid @RequestBody ProfissionalEntrada dados) {
        return servico.criar(dados);
    }

    @PutMapping("/{id}")
    public ProfissionalSaida atualizar(@PathVariable Long id, @Valid @RequestBody ProfissionalEntrada dados) {
        return servico.atualizar(id, dados);
    }
}
