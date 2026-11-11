package br.com.aurora.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.config.UsuarioAutenticado;
import br.com.aurora.dto.CompetenciaDto;
import br.com.aurora.dto.ReaberturaEntrada;
import br.com.aurora.dto.ResumoCompetencia;
import br.com.aurora.dto.SituacaoCompetenciaDto;
import br.com.aurora.service.CompetenciaService;
import jakarta.validation.Valid;

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

    @GetMapping
    public List<CompetenciaDto> listar() {
        return servico.listar();
    }

    /** RF11. */
    @GetMapping("/{id}/resumo")
    public ResumoCompetencia resumo(@PathVariable Long id) {
        return servico.resumo(id);
    }

    /** RF12. */
    @PostMapping("/{id}/fechamento")
    public CompetenciaDto fechar(@PathVariable Long id) {
        return servico.fechar(id);
    }

    /** RF15. */
    @PostMapping("/{id}/reabertura")
    public CompetenciaDto reabrir(@PathVariable Long id, @Valid @RequestBody ReaberturaEntrada dados,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return servico.reabrir(id, dados.motivo(), usuario.getId());
    }
}
