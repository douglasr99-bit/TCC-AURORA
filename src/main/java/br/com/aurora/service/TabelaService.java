package br.com.aurora.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import br.com.aurora.dto.ItemTabela;
import br.com.aurora.exception.NaoEncontradoException;

/**
 * Tabelas de códigos do SIA usadas na validação e nos formulários. Fontes em
 * docs/registro-implementacao.md: tabelas S_CDN do pacote BDSIA 10/2026 (nacionalidade,
 * etnia, caráter de atendimento), S_CID do mesmo pacote (CID-10 vigentes) e layout do BPA-I (raça/cor).
 */
@Service
public class TabelaService {

    public static final String NACIONALIDADE = "nacionalidade";
    public static final String ETNIA = "etnia";
    public static final String RACA_COR = "raca_cor";
    public static final String CARATER_ATENDIMENTO = "carater_atendimento";
    public static final String CID10 = "cid10";

    private final Map<String, Map<String, String>> tabelas = new LinkedHashMap<>();

    public TabelaService() {
        for (String nome : List.of(NACIONALIDADE, ETNIA, RACA_COR, CARATER_ATENDIMENTO, CID10)) {
            tabelas.put(nome, carregar(nome));
        }
    }

    public List<ItemTabela> listar(String tabela) {
        Map<String, String> itens = tabelas.get(tabela);
        if (itens == null) {
            throw new NaoEncontradoException("Tabela não encontrada.");
        }
        return itens.entrySet().stream().map(e -> new ItemTabela(e.getKey(), e.getValue())).toList();
    }

    public boolean contem(String tabela, String codigo) {
        return codigo != null && tabelas.get(tabela).containsKey(codigo);
    }

    private static Map<String, String> carregar(String nome) {
        Map<String, String> itens = new LinkedHashMap<>();
        var recurso = new ClassPathResource("tabelas/" + nome + ".csv");
        try (var leitor = new BufferedReader(new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8))) {
            leitor.readLine(); // cabeçalho
            String linha;
            while ((linha = leitor.readLine()) != null) {
                int sep = linha.indexOf(';');
                if (sep > 0) {
                    itens.put(linha.substring(0, sep), linha.substring(sep + 1));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler a tabela " + nome, e);
        }
        return itens;
    }
}
