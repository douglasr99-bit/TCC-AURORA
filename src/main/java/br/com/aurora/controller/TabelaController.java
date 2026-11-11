package br.com.aurora.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.dto.ItemTabela;
import br.com.aurora.service.TabelaService;

/** Tabelas de códigos do SIA para preencher as listas dos formulários. */
@RestController
@RequestMapping("/api/tabelas")
public class TabelaController {

    private final TabelaService tabelas;

    public TabelaController(TabelaService tabelas) {
        this.tabelas = tabelas;
    }

    @GetMapping("/{nome}")
    public List<ItemTabela> listar(@PathVariable String nome) {
        return tabelas.listar(nome);
    }
}
