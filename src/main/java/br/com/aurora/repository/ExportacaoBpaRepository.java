package br.com.aurora.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.aurora.domain.ExportacaoBpa;
import br.com.aurora.dto.ExportacaoDto;

public interface ExportacaoBpaRepository extends JpaRepository<ExportacaoBpa, Long> {

    /** Histórico sem carregar o conteúdo dos arquivos. */
    @Query("""
            select new br.com.aurora.dto.ExportacaoDto(e.id, e.dataGeracao, c.mesAno, e.nomeArquivo,
                   e.totalRegistros, u.nome)
            from ExportacaoBpa e join e.competencia c join e.usuario u
            order by e.dataGeracao desc, e.id desc""")
    List<ExportacaoDto> historico();
}
