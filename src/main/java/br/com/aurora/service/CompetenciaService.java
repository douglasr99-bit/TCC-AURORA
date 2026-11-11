package br.com.aurora.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.ReaberturaCompetencia;
import br.com.aurora.domain.SituacaoCompetencia;
import br.com.aurora.dto.CompetenciaDto;
import br.com.aurora.dto.ProducaoProcedimento;
import br.com.aurora.dto.ProducaoProfissional;
import br.com.aurora.dto.ReaberturaDto;
import br.com.aurora.dto.ResumoCompetencia;
import br.com.aurora.dto.SituacaoCompetenciaDto;
import br.com.aurora.exception.NaoEncontradoException;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.AtendimentoRepository;
import br.com.aurora.repository.CompetenciaRepository;
import br.com.aurora.repository.ReaberturaCompetenciaRepository;
import br.com.aurora.repository.UsuarioRepository;
import br.com.aurora.validacao.Texto;

/** Competências: criação automática, conferência (RF11), fechamento (RF12) e reabertura (RF15). */
@Service
public class CompetenciaService {

    static final DateTimeFormatter AAAAMM = DateTimeFormatter.ofPattern("yyyyMM");
    static final DateTimeFormatter MM_AAAA = DateTimeFormatter.ofPattern("MM/yyyy");

    private final CompetenciaRepository repositorio;
    private final AtendimentoRepository atendimentos;
    private final ReaberturaCompetenciaRepository reaberturas;
    private final UsuarioRepository usuarios;
    private final Clock relogio;

    public CompetenciaService(CompetenciaRepository repositorio, AtendimentoRepository atendimentos,
            ReaberturaCompetenciaRepository reaberturas, UsuarioRepository usuarios, Clock relogio) {
        this.repositorio = repositorio;
        this.atendimentos = atendimentos;
        this.reaberturas = reaberturas;
        this.usuarios = usuarios;
        this.relogio = relogio;
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

    @Transactional(readOnly = true)
    public List<CompetenciaDto> listar() {
        return repositorio.findAll(Sort.by(Sort.Direction.DESC, "mesAno")).stream().map(CompetenciaDto::de).toList();
    }

    /** RF11: totais, produção por procedimento e por profissional. */
    @Transactional(readOnly = true)
    public ResumoCompetencia resumo(Long id) {
        Competencia c = carregar(id);
        List<ProducaoProcedimento> porProcedimento = atendimentos.producaoPorProcedimento(c);
        List<ProducaoProfissional> porProfissional = atendimentos.producaoPorProfissional(c);
        long bpaC = porProcedimento.stream().filter(p -> p.modalidade() == Modalidade.BPA_C)
                .mapToLong(ProducaoProcedimento::quantidade).sum();
        long bpaI = porProcedimento.stream().filter(p -> p.modalidade() == Modalidade.BPA_I)
                .mapToLong(ProducaoProcedimento::quantidade).sum();
        List<ReaberturaDto> historico = reaberturas.daCompetencia(c).stream()
                .map(r -> new ReaberturaDto(r.getDataReabertura(), r.getMotivo(), r.getUsuario().getNome()))
                .toList();
        return new ResumoCompetencia(CompetenciaDto.de(c), bpaC + bpaI, bpaC, bpaI, porProfissional.size(),
                porProcedimento, porProfissional, historico);
    }

    /** RF12: só ABERTA → FECHADA; bloqueia lançamentos e alterações. */
    @Transactional
    public CompetenciaDto fechar(Long id) {
        Competencia c = carregar(id);
        if (fechada(c)) {
            throw new NegocioException(null, "A competência " + formatar(c.getMesAno()) + " já está fechada.");
        }
        c.setSituacao(SituacaoCompetencia.FECHADA);
        c.setDataFechamento(LocalDateTime.now(relogio));
        return CompetenciaDto.de(c);
    }

    /** RF15: só FECHADA → ABERTA, com motivo; registra data, usuário e motivo. */
    @Transactional
    public CompetenciaDto reabrir(Long id, String motivo, Long idUsuario) {
        Competencia c = carregar(id);
        if (!fechada(c)) {
            throw new NegocioException(null, "A competência " + formatar(c.getMesAno()) + " já está aberta.");
        }
        String texto = Texto.limpo(motivo);
        if (texto == null) {
            throw new NegocioException("motivo", "Informe o motivo da reabertura.");
        }
        ReaberturaCompetencia r = new ReaberturaCompetencia();
        r.setCompetencia(c);
        r.setUsuario(usuarios.getReferenceById(idUsuario));
        r.setDataReabertura(LocalDateTime.now(relogio));
        r.setMotivo(texto);
        reaberturas.save(r);

        c.setSituacao(SituacaoCompetencia.ABERTA);
        c.setDataFechamento(null);
        return CompetenciaDto.de(c);
    }

    Competencia carregar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new NaoEncontradoException("Competência não encontrada."));
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
