package br.com.aurora.dto;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/** Item do histórico de exportações (RF14). */
public record ExportacaoDto(Long id, LocalDateTime dataGeracao, String competencia, String nomeArquivo,
        int totalRegistros, String usuario) {

    public ExportacaoDto(Long id, LocalDateTime dataGeracao, YearMonth competencia, String nomeArquivo,
            int totalRegistros, String usuario) {
        this(id, dataGeracao, competencia.format(DateTimeFormatter.ofPattern("yyyyMM")), nomeArquivo,
                totalRegistros, usuario);
    }
}
