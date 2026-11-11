package br.com.aurora.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.config.UsuarioAutenticado;
import br.com.aurora.dto.AtendimentoEntrada;
import br.com.aurora.dto.AtendimentoSaida;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.service.AtendimentoService;
import jakarta.validation.Valid;

/** RF07 e RF10. */
@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

    private final AtendimentoService servico;

    public AtendimentoController(AtendimentoService servico) {
        this.servico = servico;
    }

    /** {@code ?competencia=AAAAMM} (consulta da competência) ou {@code ?data=AAAA-MM-DD} (lançamentos do dia). */
    @GetMapping
    public List<AtendimentoSaida> listar(@RequestParam(required = false) String competencia,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        if (competencia != null) {
            return servico.listarDaCompetencia(competencia);
        }
        if (data != null) {
            return servico.listarDaData(data);
        }
        throw new NegocioException("competencia", "Informe a competência (AAAAMM) ou a data.");
    }

    @GetMapping("/{id}")
    public AtendimentoSaida obter(@PathVariable Long id) {
        return servico.obter(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AtendimentoSaida lancar(@Valid @RequestBody AtendimentoEntrada dados,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.lancar(dados, usuario.getId());
    }

    @PutMapping("/{id}")
    public AtendimentoSaida atualizar(@PathVariable Long id, @Valid @RequestBody AtendimentoEntrada dados) {
        return servico.atualizar(id, dados);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) {
        servico.excluir(id);
    }
}
