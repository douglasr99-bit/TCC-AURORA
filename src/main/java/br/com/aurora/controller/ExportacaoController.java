package br.com.aurora.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.aurora.config.UsuarioAutenticado;
import br.com.aurora.dto.ExportacaoDto;
import br.com.aurora.service.ExportacaoBpaService;
import br.com.aurora.service.ExportacaoBpaService.ArquivoGerado;

/** RF13 e RF14. Nomes conforme o diagrama de sequência do TCC. */
@RestController
public class ExportacaoController {

    private final ExportacaoBpaService servico;

    public ExportacaoController(ExportacaoBpaService servico) {
        this.servico = servico;
    }

    /** Gera o arquivo BPA da competência (só FECHADA) e devolve para download. */
    @PostMapping("/api/competencias/{id}/exportacao")
    public ResponseEntity<byte[]> gerar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return download(servico.gerarArquivo(id, usuario.getId()));
    }

    @GetMapping("/api/exportacoes")
    public List<ExportacaoDto> historico() {
        return servico.historico();
    }

    @GetMapping("/api/exportacoes/{id}/arquivo")
    public ResponseEntity<byte[]> baixar(@PathVariable Long id) {
        return download(servico.arquivo(id));
    }

    /** Arquivo texto ASCII, como pede o layout do BPA. */
    private static ResponseEntity<byte[]> download(ArquivoGerado arquivo) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "plain", StandardCharsets.US_ASCII))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(arquivo.nomeArquivo()).build().toString())
                .header("X-Exportacao-Id", String.valueOf(arquivo.id()))
                .body(arquivo.conteudo().getBytes(StandardCharsets.US_ASCII));
    }
}
