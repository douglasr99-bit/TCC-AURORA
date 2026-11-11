package br.com.aurora.atendimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.config.UsuarioAutenticado;
import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.Estabelecimento;
import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.Perfil;
import br.com.aurora.domain.Procedimento;
import br.com.aurora.domain.Profissional;
import br.com.aurora.domain.SituacaoCompetencia;
import br.com.aurora.domain.Sexo;
import br.com.aurora.domain.Usuario;
import br.com.aurora.repository.CompetenciaRepository;
import br.com.aurora.repository.EstabelecimentoRepository;
import br.com.aurora.repository.PacienteRepository;
import br.com.aurora.repository.ProcedimentoRepository;
import br.com.aurora.repository.ProfissionalRepository;
import br.com.aurora.repository.ReaberturaCompetenciaRepository;
import br.com.aurora.repository.UsuarioRepository;

/** Conferência, fechamento, reabertura e geração do arquivo BPA no PostgreSQL de teste. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompetenciaExportacaoTest {

    private static final YearMonth MES = YearMonth.of(2025, 4);

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProfissionalRepository profissionais;
    @Autowired ProcedimentoRepository procedimentos;
    @Autowired PacienteRepository pacientes;
    @Autowired CompetenciaRepository competencias;
    @Autowired EstabelecimentoRepository estabelecimentos;
    @Autowired ReaberturaCompetenciaRepository reaberturas;

    private UsuarioAutenticado responsavel;
    private UsuarioAutenticado operador;
    private Usuario usuarioResponsavel;
    private Profissional prof;
    private Procedimento procC;
    private Procedimento procI;
    private Paciente paciente;
    private Competencia competencia;

    @BeforeEach
    void preparar() {
        usuarioResponsavel = usuario("it.resp", Perfil.RESPONSAVEL_PRODUCAO);
        responsavel = new UsuarioAutenticado(usuarioResponsavel);
        operador = new UsuarioAutenticado(usuario("it.oper", Perfil.OPERADOR));

        if (estabelecimentos.count() == 0) {
            Estabelecimento e = new Estabelecimento();
            e.setCnes("7654321"); e.setNome("UBS FICTICIA"); e.setSigla("UBSF"); e.setCnpj("11222333000181");
            e.setOrgaoDestino("SMS FICTICIA"); e.setIndicadorDestino('M');
            estabelecimentos.save(e);
        }
        prof = new Profissional();
        prof.setNome("Carla Ficticia"); prof.setCns("100000000000007"); prof.setCbo("225142");
        profissionais.save(prof);
        procC = procedimento("0101010010", Modalidade.BPA_C);
        procI = procedimento("0301010072", Modalidade.BPA_I);
        paciente = new Paciente();
        paciente.setCns("800000000000001"); paciente.setNome("PACIENTE FICTICIO");
        paciente.setDataNascimento(LocalDate.of(1990, 1, 1)); paciente.setSexo(Sexo.F); paciente.setRacaCor("01");
        paciente.setNacionalidade("010"); paciente.setCodigoMunicipio("355030");
        pacientes.save(paciente);

        competencia = new Competencia();
        competencia.setMesAno(MES);
        competencias.save(competencia);
    }

    private Usuario usuario(String login, Perfil perfil) {
        Usuario u = new Usuario();
        u.setNome("Usuário Fictício " + login); u.setLogin(login); u.setSenhaHash("x"); u.setPerfis(Set.of(perfil));
        return usuarios.save(u);
    }

    private Procedimento procedimento(String codigo, Modalidade m) {
        Procedimento p = new Procedimento();
        p.setCodigo(codigo); p.setDescricao("PROCEDIMENTO " + codigo); p.setModalidade(m);
        return procedimentos.save(p);
    }

    /** Insere atendimentos direto no banco (em lote), alternando BPA-C e BPA-I. */
    private void inserirAtendimentos(int quantidade) {
        competencias.flush();
        List<Object[]> linhas = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            boolean individual = i % 3 == 0;
            linhas.add(new Object[] {competencia.getId(), prof.getId(), individual ? procI.getId() : procC.getId(),
                    individual ? paciente.getId() : null, usuarioResponsavel.getId(),
                    Date.valueOf(MES.atDay(1 + i % 28)), 1 + i % 3, individual ? 35 : i % 90,
                    individual ? "J069" : null, individual ? "01" : null});
        }
        jdbc.batchUpdate("""
                INSERT INTO atendimento (competencia_id, profissional_id, procedimento_id, paciente_id, usuario_id,
                    data_atendimento, quantidade, idade, cid, carater_atendimento)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""", linhas);
    }

    private String url(String sufixo) {
        return "/api/competencias/" + competencia.getId() + sufixo;
    }

    @Test
    @DisplayName("T28 Conferência: totais por modalidade, por procedimento e por profissional")
    void conferencia() throws Exception {
        inserirAtendimentos(6);   // i=0,3 → BPA-I (qt 1,1); i=1,2,4,5 → BPA-C (qt 2,3,2,3)
        mvc.perform(get(url("/resumo")).with(user(responsavel)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBpaI").value(2))
                .andExpect(jsonPath("$.totalBpaC").value(10))
                .andExpect(jsonPath("$.totalAtendimentos").value(12))
                .andExpect(jsonPath("$.profissionais").value(1))
                .andExpect(jsonPath("$.porProcedimento.length()").value(2));
    }

    @Test
    @DisplayName("T22 Gerar arquivo com a competência ABERTA → recusado com mensagem")
    void gerarComCompetenciaAberta() throws Exception {
        inserirAtendimentos(3);
        mvc.perform(post(url("/exportacao")).with(user(responsavel)).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Feche a competência antes de gerar o arquivo."));
    }

    @Test
    @DisplayName("T27 Reabrir sem motivo → erro; com motivo → registra a reabertura e volta para ABERTA")
    void reabrir() throws Exception {
        mvc.perform(post(url("/fechamento")).with(user(responsavel)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("FECHADA"))
                .andExpect(jsonPath("$.dataFechamento").isNotEmpty());

        mvc.perform(post(url("/reabertura")).with(user(responsavel)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("motivo"));
        assertThat(competencias.findById(competencia.getId()).orElseThrow().getSituacao())
                .isEqualTo(SituacaoCompetencia.FECHADA);

        mvc.perform(post(url("/reabertura")).with(user(responsavel)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Corrigir lançamentos glosados\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").value("ABERTA"));
        var registradas = reaberturas.daCompetencia(competencias.findById(competencia.getId()).orElseThrow());
        assertThat(registradas).hasSize(1);
        assertThat(registradas.get(0).getMotivo()).isEqualTo("Corrigir lançamentos glosados");
        assertThat(registradas.get(0).getUsuario().getId()).isEqualTo(usuarioResponsavel.getId());
    }

    @Test
    @DisplayName("Fechar competência já fechada e reabrir competência aberta → recusados")
    void transicoesInvalidas() throws Exception {
        mvc.perform(post(url("/reabertura")).with(user(responsavel)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"teste\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(url("/fechamento")).with(user(responsavel)).with(csrf())).andExpect(status().isOk());
        mvc.perform(post(url("/fechamento")).with(user(responsavel)).with(csrf())).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Operador não fecha competência nem gera arquivo → 403")
    void operadorSemPermissao() throws Exception {
        mvc.perform(post(url("/fechamento")).with(user(operador)).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post(url("/exportacao")).with(user(operador)).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T29 RNF07: 3.000 atendimentos geram o arquivo em até 10 s; histórico permite baixar de novo")
    void desempenhoEHistorico() throws Exception {
        inserirAtendimentos(3000);
        mvc.perform(post(url("/fechamento")).with(user(responsavel)).with(csrf())).andExpect(status().isOk());

        long inicio = System.nanoTime();
        var resposta = mvc.perform(post(url("/exportacao")).with(user(responsavel)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"bpa_202504.txt\""))
                .andReturn().getResponse();
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        System.out.println("RNF07: arquivo de 3.000 atendimentos gerado em " + ms + " ms");
        assertThat(ms).isLessThan(10_000);

        String conteudo = new String(resposta.getContentAsByteArray(), StandardCharsets.US_ASCII);
        String[] linhas = conteudo.split("\r\n");
        int registros = Integer.parseInt(linhas[0].substring(13, 19));
        assertThat(linhas).hasSize(registros + 1);
        // 1.000 BPA-I (i múltiplo de 3) + BPA-C consolidado: i não múltiplo de 3 → i % 90 só assume
        // as 60 idades que não são múltiplas de 3 (90 é múltiplo de 3).
        assertThat(registros).isEqualTo(1000 + 60);

        String id = resposta.getHeader("X-Exportacao-Id");
        mvc.perform(get("/api/exportacoes").with(user(responsavel)))
                .andExpect(jsonPath("$[0].nomeArquivo").value("bpa_202504.txt"))
                .andExpect(jsonPath("$[0].totalRegistros").value(registros));
        var baixado = mvc.perform(get("/api/exportacoes/" + id + "/arquivo").with(user(responsavel)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(baixado, StandardCharsets.US_ASCII)).isEqualTo(conteudo);
    }
}
