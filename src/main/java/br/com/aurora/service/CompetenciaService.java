package br.com.aurora.service;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.SituacaoCompetencia;
import br.com.aurora.dto.SituacaoCompetenciaDto;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.CompetenciaRepository;

@Service
public class CompetenciaService {

    static final DateTimeFormatter AAAAMM = DateTimeFormatter.ofPattern("yyyyMM");
    static final DateTimeFormatter MM_AAAA = DateTimeFormatter.ofPattern("MM/yyyy");

    private final CompetenciaRepository repositorio;

    public CompetenciaService(CompetenciaRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Competência do mês; criada ABERTA no primeiro lançamento (RF07). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Competencia obterOuCriar(YearMonth mesAno) {
        repositorio.criarSeNaoExistir(mesAno.format(AAAAMM));
        return repositorio.findByMesAno(mesAno).orElseThrow();
    }

    @Transactional(readOnly = true)
    public SituacaoCompetenciaDto situacao(String mesAno) {
        YearMonth ym = interpretar(mesAno, "mesAno");
        return new SituacaoCompetenciaDto(ym.format(AAAAMM),
                repositorio.findByMesAno(ym).map(Competencia::getSituacao).orElse(null));
    }

    /** Converte AAAAMM em {@link YearMonth}, com mensagem clara se vier errado. */
    public static YearMonth interpretar(String mesAno, String campo) {
        if (mesAno == null || !mesAno.matches("\\d{4}(0[1-9]|1[0-2])")) {
            throw new NegocioException(campo, "Competência inválida. Use o formato AAAAMM (ex.: 202611).");
        }
        return YearMonth.parse(mesAno, AAAAMM);
    }

    public static String formatar(YearMonth mesAno) {
        return mesAno.format(MM_AAAA);
    }

    public static boolean fechada(Competencia c) {
        return c.getSituacao() == SituacaoCompetencia.FECHADA;
    }
}
