package br.com.aurora.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.Sexo;
import br.com.aurora.dto.PacienteEntrada;
import br.com.aurora.exception.ErroCampo;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.PacienteRepository;

/** Regras do cadastro de paciente (RF05). Só dados fictícios. */
class PacienteServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 11, 11);

    private PacienteRepository repositorio;
    private PacienteService servico;

    @BeforeEach
    void preparar() {
        repositorio = mock(PacienteRepository.class);
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));
        Clock relogio = Clock.fixed(HOJE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        servico = new PacienteService(repositorio, new TabelaService(), relogio);
    }

    private static PacienteEntrada paciente(LocalDate nascimento, String raca, String nacionalidade, String etnia) {
        return new PacienteEntrada("100000000000007", "MARIA FICTICIA DA SILVA", nascimento, Sexo.F,
                raca, etnia, nacionalidade, "355030", null, null, null, null, null);
    }

    private static String mensagemDo(NegocioException e, String campo) {
        return e.getErros().stream().filter(x -> campo.equals(x.campo())).map(ErroCampo::mensagem)
                .findFirst().orElse(null);
    }

    @Test
    @DisplayName("Paciente válido é salvo")
    void valido() {
        var salvo = servico.criar(paciente(LocalDate.of(1980, 5, 20), "03", "010", null));
        assertThat(salvo.nome()).isEqualTo("MARIA FICTICIA DA SILVA");
        verify(repositorio).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Data de nascimento no futuro é recusada")
    void nascimentoNoFuturo() {
        var e = (NegocioException) org.junit.jupiter.api.Assertions.assertThrows(NegocioException.class,
                () -> servico.criar(paciente(HOJE.plusDays(1), "01", "010", null)));
        assertThat(mensagemDo(e, "dataNascimento")).contains("futuro");
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Idade acima de 130 anos é recusada (domínio do layout do BPA)")
    void idadeAcimaDe130() {
        assertThatThrownBy(() -> servico.criar(paciente(LocalDate.of(1890, 1, 1), "01", "010", null)))
                .isInstanceOf(NegocioException.class);
    }

    @Test
    @DisplayName("Indígena brasileiro sem etnia → erro no campo etnia")
    void indigenaSemEtnia() {
        var e = (NegocioException) org.junit.jupiter.api.Assertions.assertThrows(NegocioException.class,
                () -> servico.criar(paciente(LocalDate.of(1990, 1, 1), "05", "010", null)));
        assertThat(mensagemDo(e, "etnia")).contains("escolha a etnia");
    }

    @Test
    @DisplayName("Indígena brasileiro com etnia da tabela (inclusive código X999) é aceito")
    void indigenaComEtnia() {
        servico.criar(paciente(LocalDate.of(1990, 1, 1), "05", "010", "0001"));
        servico.criar(paciente(LocalDate.of(1990, 1, 1), "05", "010", "X411"));
    }

    @Test
    @DisplayName("Etnia informada para quem não é indígena → erro")
    void etniaSemSerIndigena() {
        var e = (NegocioException) org.junit.jupiter.api.Assertions.assertThrows(NegocioException.class,
                () -> servico.criar(paciente(LocalDate.of(1990, 1, 1), "01", "010", "0001")));
        assertThat(mensagemDo(e, "etnia")).contains("só é informada");
    }

    @Test
    @DisplayName("Raça/cor e nacionalidade fora das tabelas → erro em cada campo")
    void codigosForaDaTabela() {
        var e = (NegocioException) org.junit.jupiter.api.Assertions.assertThrows(NegocioException.class,
                () -> servico.criar(paciente(LocalDate.of(1990, 1, 1), "07", "999", null)));
        assertThat(mensagemDo(e, "racaCor")).isNotNull();
        assertThat(mensagemDo(e, "nacionalidade")).isNotNull();
    }

    @Test
    @DisplayName("CNS já cadastrado → erro no campo cns")
    void cnsDuplicado() {
        when(repositorio.existsByCns("100000000000007")).thenReturn(true);
        var e = (NegocioException) org.junit.jupiter.api.Assertions.assertThrows(NegocioException.class,
                () -> servico.criar(paciente(LocalDate.of(1990, 1, 1), "01", "010", null)));
        assertThat(mensagemDo(e, "cns")).contains("Já existe");
    }
}
