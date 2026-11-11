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

import br.com.aurora.dto.ProcedimentoEntrada;
import br.com.aurora.dto.ProcedimentoSaida;
import br.com.aurora.service.ProcedimentoService;
import jakarta.validation.Valid;

/** RF06. */
@RestController
@RequestMapping("/api/procedimentos")
public class ProcedimentoController {

    private final ProcedimentoService servico;

    public ProcedimentoController(ProcedimentoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<ProcedimentoSaida> buscar(@RequestParam(required = false) String q) {
        return servico.buscar(q);
    }

    @GetMapping("/{id}")
    public ProcedimentoSaida obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProcedimentoSaida criar(@Valid @RequestBody ProcedimentoEntrada dados) {
        return servico.criar(dados);
    }

    @PutMapping("/{id}")
    public ProcedimentoSaida atualizar(@PathVariable Long id, @Valid @RequestBody ProcedimentoEntrada dados) {
        return servico.atualizar(id, dados);
    }
}
