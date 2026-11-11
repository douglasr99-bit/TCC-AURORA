package br.com.aurora.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.dto.SituacaoCompetenciaDto;
import br.com.aurora.service.CompetenciaService;

@RestController
@RequestMapping("/api/competencias")
public class CompetenciaController {

    private final CompetenciaService servico;

    public CompetenciaController(CompetenciaService servico) {
        this.servico = servico;
    }

    /** Situação de uma competência, para a barra superior (todos os perfis). */
    @GetMapping("/situacao")
    public SituacaoCompetenciaDto situacao(@RequestParam String mesAno) {
        return servico.situacao(mesAno);
    }
}
