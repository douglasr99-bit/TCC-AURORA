package br.com.aurora.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.aurora.domain.Atendimento;
import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.Procedimento;
import br.com.aurora.domain.Profissional;
import br.com.aurora.domain.SituacaoCompetencia;
import br.com.aurora.domain.Usuario;
import br.com.aurora.dto.AtendimentoEntrada;
import br.com.aurora.exception.ErroCampo;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.AtendimentoRepository;
import br.com.aurora.repository.CompetenciaRepository;
import br.com.aurora.repository.PacienteRepository;
import br.com.aurora.repository.ProcedimentoRepository;
import br.com.aurora.repository.ProfissionalRepository;
import br.com.aurora.repository.UsuarioRepository;

/** Regras do lançamento (RF07, RF08) com repositórios simulados. Só dados fictícios. */
class AtendimentoServiceTest {

    private static final long PROF = 1, PROF_INATIVO = 2, PROC_C = 10, PROC_I_CID = 11, PAC = 20;

    private AtendimentoRepository atendimentos;
    private CompetenciaService competenciaService;
    private Competencia competencia;

    private AtendimentoService servicoEm(LocalDate hoje) {
        Clock relogio = Clock.fixed(hoje.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        ProfissionalRepository profissionais = mock(ProfissionalRepository.class);
        ProcedimentoRepository procedimentos = mock(ProcedimentoRepository.class);
        PacienteRepository pacientes = mock(PacienteRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);

        Profissional ativo = new Profissional();
        ativo.setNome("Ana Ficticia"); ativo.setCns("100000000000007"); ativo.setCbo("225125"); ativo.setAtivo(true);
        Profissional inativo = new Profissional();
        inativo.setNome("Bruno Ficticio"); inativo.setCbo("225125"); inativo.setAtivo(false);
        when(profissionais.findById(PROF)).thenReturn(Optional.of(ativo));
        when(profissionais.findById(PROF_INATIVO)).thenReturn(Optional.of(inativo));

        Procedimento bpaC = new Procedimento();
        bpaC.setCodigo("0101010010"); bpaC.setDescricao("ATIVIDADE EDUCATIVA"); bpaC.setModalidade(Modalidade.BPA_C);
        Procedimento bpaICid = new Procedimento();
        bpaICid.setCodigo("0301010072"); bpaICid.setDescricao("CONSULTA"); bpaICid.setModalidade(Modalidade.BPA_I);
        bpaICid.setExigeCid(true);
        when(procedimentos.findById(PROC_C)).thenReturn(Optional.of(bpaC));
        when(procedimentos.findById(PROC_I_CID)).thenReturn(Optional.of(bpaICid));

        Paciente paciente = new Paciente();
        paciente.setNome("JOAO FICTICIO"); paciente.setCns("800000000000001");
        paciente.setDataNascimento(LocalDate.of(1980, 6, 15));
        when(pacientes.findById(PAC)).thenReturn(Optional.of(paciente));
        when(usuarios.getReferenceById(any())).thenReturn(new Usuario());

        return new AtendimentoService(atendimentos, mock(CompetenciaRepository.class), competenciaService,
                profissionais, procedimentos, pacientes, usuarios, new TabelaService(), relogio);
    }

    @BeforeEach
    void preparar() {
        atendimentos = mock(AtendimentoRepository.class);
        when(atendimentos.save(any())).thenAnswer(i -> i.getArgument(0));
        competenciaService = mock(CompetenciaService.class);
        competencia = new Competencia();
        competencia.setMesAno(YearMonth.of(2026, 11));
        when(competenciaService.obterOuCriar(any())).thenAnswer(i -> {
            ReflectionTestUtils.setField(competencia, "mesAno", i.getArgument(0));
            return competencia;
        });
    }

    private static AtendimentoEntrada bpaC(LocalDate data, Integer idade) {
        return new AtendimentoEntrada(data, PROF, PROC_C, 3, null, idade, null, null);
    }

    private static AtendimentoEntrada bpaI(LocalDate data, Long paciente, String cid, String carater) {
        return new AtendimentoEntrada(data, PROF, PROC_I_CID, 1, paciente, null, cid, carater);
    }

    private static String erroEm(NegocioException e, String campo) {
        return e.getErros().stream().filter(x -> campo.equals(x.campo())).map(ErroCampo::mensagem)
                .findFirst().orElse(null);
    }

    @Test
    @DisplayName("T05 BPA-I sem paciente → erro no campo do paciente")
    void bpaISemPaciente() {
        var e = assertThrows(NegocioException.class,
                () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(bpaI(LocalDate.of(2026, 11, 10), null, "J069", "01"), 1L));
        assertThat(erroEm(e, "pacienteId")).contains("BPA-I");
        verify(atendimentos, never()).save(any());
    }

    @Test
    @DisplayName("T06 BPA-C sem idade → erro no campo idade")
    void bpaCSemIdade() {
        var e = assertThrows(NegocioException.class,
                () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(bpaC(LocalDate.of(2026, 11, 10), null), 1L));
        assertThat(erroEm(e, "idade")).contains("Informe a idade");
    }

    @Test
    @DisplayName("BPA-C com idade fora de 0–130 → erro no campo idade")
    void bpaCIdadeForaDoDominio() {
        var e = assertThrows(NegocioException.class,
                () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(bpaC(LocalDate.of(2026, 11, 10), 131), 1L));
        assertThat(erroEm(e, "idade")).contains("entre 0 e 130");
    }

    @Test
    @DisplayName("T07 Procedimento que exige CID, sem CID → erro no campo cid")
    void exigeCidSemCid() {
        var e = assertThrows(NegocioException.class,
                () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(bpaI(LocalDate.of(2026, 11, 10), PAC, null, "01"), 1L));
        assertThat(erroEm(e, "cid")).contains("exige o CID-10");
    }

    @Test
    @DisplayName("CID inexistente na tabela do SIA → erro no campo cid")
    void cidInexistente() {
        var e = assertThrows(NegocioException.class,
                () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(bpaI(LocalDate.of(2026, 11, 10), PAC, "A999", "01"), 1L));
        assertThat(erroEm(e, "cid")).contains("não encontrado");
    }

    @Test
    @DisplayName("BPA-I sem caráter de atendimento → erro no campo")
    void bpaISemCarater() {
        var e = assertThrows(NegocioException.class,
                () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(bpaI(LocalDate.of(2026, 11, 10), PAC, "J069", null), 1L));
        assertThat(erroEm(e, "caraterAtendimento")).isNotNull();
    }

    @Test
    @DisplayName("T10 Data do atendimento no futuro → erro no campo data")
    void dataNoFuturo() {
        var e = assertThrows(NegocioException.class,
                () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(bpaC(LocalDate.of(2026, 11, 12), 30), 1L));
        assertThat(erroEm(e, "data")).contains("futuro");
    }

    @Test
    @DisplayName("Profissional inativo → erro no campo do profissional")
    void profissionalInativo() {
        var entrada = new AtendimentoEntrada(LocalDate.of(2026, 11, 10), PROF_INATIVO, PROC_C, 1, null, 30, null, null);
        var e = assertThrows(NegocioException.class, () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(entrada, 1L));
        assertThat(erroEm(e, "profissionalId")).contains("inativo");
    }

    @Test
    @DisplayName("Vários problemas no mesmo lançamento → todos apontados de uma vez")
    void variosErros() {
        var entrada = new AtendimentoEntrada(LocalDate.of(2026, 11, 12), PROF_INATIVO, PROC_I_CID, 1, null, null, null, null);
        var e = assertThrows(NegocioException.class, () -> servicoEm(LocalDate.of(2026, 11, 11)).lancar(entrada, 1L));
        assertThat(e.getErros()).extracting(ErroCampo::campo)
                .contains("data", "profissionalId", "pacienteId", "caraterAtendimento", "cid");
    }

    @Test
    @DisplayName("BPA-I válido: idade calculada pela data de nascimento na data do atendimento")
    void bpaIValidoCalculaIdade() {
        var salvo = servicoEm(LocalDate.of(2026, 11, 11)).lancar(bpaI(LocalDate.of(2026, 6, 14), PAC, "J069", "01"), 1L);
        assertThat(salvo.idade()).isEqualTo(45); // nasceu em 15/06/1980; em 14/06/2026 ainda tem 45
        assertThat(salvo.caraterAtendimento()).isEqualTo("01");
    }

    @Test
    @DisplayName("T12a Atendimento de novembro lançado em dezembro entra na competência de novembro (aberta)")
    void novembroLancadoEmDezembro() {
        var salvo = servicoEm(LocalDate.of(2026, 12, 3)).lancar(bpaC(LocalDate.of(2026, 11, 28), 40), 1L);
        verify(competenciaService).obterOuCriar(YearMonth.of(2026, 11));
        assertThat(salvo.competencia()).isEqualTo("202611");
    }

    @Test
    @DisplayName("T12b Atendimento de novembro lançado em dezembro é recusado se novembro está FECHADA")
    void novembroFechado() {
        competencia.setSituacao(SituacaoCompetencia.FECHADA);
        var e = assertThrows(NegocioException.class,
                () -> servicoEm(LocalDate.of(2026, 12, 3)).lancar(bpaC(LocalDate.of(2026, 11, 28), 40), 1L));
        assertThat(erroEm(e, "data")).contains("11/2026 está fechada").contains("reabra");
        verify(atendimentos, never()).save(any(Atendimento.class));
    }
}
