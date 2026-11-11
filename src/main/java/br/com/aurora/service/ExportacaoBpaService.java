package br.com.aurora.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.bpa.ArquivoBpa;
import br.com.aurora.bpa.ArquivoBpaException;
import br.com.aurora.bpa.GeradorArquivoBpa;
import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.Estabelecimento;
import br.com.aurora.domain.ExportacaoBpa;
import br.com.aurora.dto.ExportacaoDto;
import br.com.aurora.exception.NaoEncontradoException;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.AtendimentoRepository;
import br.com.aurora.repository.EstabelecimentoRepository;
import br.com.aurora.repository.ExportacaoBpaRepository;
import br.com.aurora.repository.UsuarioRepository;

/**
 * Geração do arquivo BPA (RF13) e histórico de exportações (RF14). Nomes conforme o
 * diagrama de sequência do TCC.
 */
@Service
public class ExportacaoBpaService {

    private final CompetenciaService competencias;
    private final EstabelecimentoRepository estabelecimentos;
    private final AtendimentoRepository atendimentos;
    private final ExportacaoBpaRepository exportacoes;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    public ExportacaoBpaService(CompetenciaService competencias, EstabelecimentoRepository estabelecimentos,
            AtendimentoRepository atendimentos, ExportacaoBpaRepository exportacoes, UsuarioRepository usuarios,
            Clock relogio) {
        this.competencias = competencias;
        this.estabelecimentos = estabelecimentos;
        this.atendimentos = atendimentos;
        this.exportacoes = exportacoes;
        this.usuarios = usuarios;
        this.relogio = relogio;
    }

    /** Arquivo pronto para download. */
    public record ArquivoGerado(Long id, String nomeArquivo, String conteudo) {
    }

    @Transactional
    public ArquivoGerado gerarArquivo(Long idCompetencia, Long idUsuario) {
        // Buscar competência e estabelecimento
        Competencia competencia = competencias.carregar(idCompetencia);
        Estabelecimento estabelecimento = estabelecimentos.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new NegocioException(null, "Cadastre o estabelecimento antes de gerar o arquivo: "
                        + "os dados dele formam o cabeçalho do BPA."));

        // Competência aberta: recusa
        if (!CompetenciaService.fechada(competencia)) {
            throw new NegocioException(null, "Feche a competência antes de gerar o arquivo.");
        }

        // Buscar atendimentos (uma consulta) e montar cabeçalho, BPA-C e BPA-I
        var lista = atendimentos.listarDaCompetencia(competencia);
        if (lista.isEmpty()) {
            throw new NegocioException(null, "A competência " + CompetenciaService.formatar(competencia.getMesAno())
                    + " não tem atendimentos lançados.");
        }
        ArquivoBpa arquivo;
        try {
            arquivo = GeradorArquivoBpa.gerar(estabelecimento, competencia.getMesAno(), lista);
        } catch (ArquivoBpaException e) {
            throw new NegocioException(null, e.getMessage());
        }

        // Salvar ExportacaoBpa
        ExportacaoBpa e = new ExportacaoBpa();
        e.setCompetencia(competencia);
        e.setUsuario(usuarios.getReferenceById(idUsuario));
        e.setDataGeracao(LocalDateTime.now(relogio));
        e.setNomeArquivo("bpa_" + competencia.getMesAno().format(CompetenciaService.AAAAMM) + ".txt");
        e.setConteudo(arquivo.conteudo());
        e.setTotalRegistros(arquivo.registros());
        exportacoes.save(e);

        return new ArquivoGerado(e.getId(), e.getNomeArquivo(), e.getConteudo());
    }

    @Transactional(readOnly = true)
    public List<ExportacaoDto> historico() {
        return exportacoes.historico();
    }

    /** RF14: baixar de novo um arquivo já gerado. */
    @Transactional(readOnly = true)
    public ArquivoGerado arquivo(Long idExportacao) {
        ExportacaoBpa e = exportacoes.findById(idExportacao)
                .orElseThrow(() -> new NaoEncontradoException("Exportação não encontrada."));
        return new ArquivoGerado(e.getId(), e.getNomeArquivo(), e.getConteudo());
    }
}
