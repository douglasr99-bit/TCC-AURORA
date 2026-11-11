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

import br.com.aurora.dto.PacienteEntrada;
import br.com.aurora.dto.PacienteSaida;
import br.com.aurora.service.PacienteService;
import jakarta.validation.Valid;

/** RF05. */
@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService servico;

    public PacienteController(PacienteService servico) {
        this.servico = servico;
    }

    @GetMapping
    public List<PacienteSaida> buscar(@RequestParam(required = false) String q) {
        return servico.buscar(q);
    }

    @GetMapping("/{id}")
    public PacienteSaida obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteSaida criar(@Valid @RequestBody PacienteEntrada dados) {
        return servico.criar(dados);
    }

    @PutMapping("/{id}")
    public PacienteSaida atualizar(@PathVariable Long id, @Valid @RequestBody PacienteEntrada dados) {
        return servico.atualizar(id, dados);
    }
}
