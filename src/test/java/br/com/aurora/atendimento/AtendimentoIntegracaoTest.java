package br.com.aurora.atendimento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.config.UsuarioAutenticado;
import br.com.aurora.domain.Competencia;
import br.com.aurora.domain.Modalidade;
import br.com.aurora.domain.Paciente;
import br.com.aurora.domain.Perfil;
import br.com.aurora.domain.Procedimento;
import br.com.aurora.domain.Profissional;
import br.com.aurora.domain.SituacaoCompetencia;
import br.com.aurora.domain.Sexo;
import br.com.aurora.domain.Usuario;
import br.com.aurora.repository.AtendimentoRepository;
import br.com.aurora.repository.CompetenciaRepository;
import br.com.aurora.repository.PacienteRepository;
import br.com.aurora.repository.ProcedimentoRepository;
import br.com.aurora.repository.ProfissionalRepository;
import br.com.aurora.repository.UsuarioRepository;

/** Lançamento de ponta a ponta no PostgreSQL de teste (RF07, RF08, RF10). Desfeito ao final. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AtendimentoIntegracaoTest {

    // Mês antigo, para não colidir com competências de uso real do banco de teste.
    private static final YearMonth MES = YearMonth.of(2025, 3);

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProfissionalRepository profissionais;
    @Autowired ProcedimentoRepository procedimentos;
    @Autowired PacienteRepository pacientes;
    @Autowired CompetenciaRepository competencias;
    @Autowired AtendimentoRepository atendimentos;

    private UsuarioAutenticado operador;
    private UsuarioAutenticado admin;
    private Long profissionalId;
    private Long procedimentoC;
    private Long procedimentoI;
    private Long pacienteId;

    @BeforeEach
    void preparar() {
        operador = new UsuarioAutenticado(usuario("it.op", Perfil.OPERADOR));
        admin = new UsuarioAutenticado(usuario("it.adm", Perfil.ADMINISTRADOR));

        Profissional prof = new Profissional();
        prof.setNome("Ana Ficticia Souza"); prof.setCns("100000000000007"); prof.setCbo("225142");
        profissionalId = profissionais.save(prof).getId();

        procedimentoC = procedimento("0101010010", Modalidade.BPA_C, false);
        procedimentoI = procedimento("0301010072", Modalidade.BPA_I, true);

        Paciente p = new Paciente();
        p.setCns("800000000000001"); p.setNome("JOAO FICTICIO"); p.setDataNascimento(LocalDate.of(1990, 1, 1));
        p.setSexo(Sexo.M); p.setRacaCor("03"); p.setNacionalidade("010"); p.setCodigoMunicipio("355030");
        pacienteId = pacientes.save(p).getId();
    }

    private Usuario usuario(String login, Perfil perfil) {
        Usuario u = new Usuario();
        u.setNome("Usuário Fictício"); u.setLogin(login); u.setSenhaHash("x"); u.setPerfis(Set.of(perfil));
        return usuarios.save(u);
    }

    private Long procedimento(String codigo, Modalidade modalidade, boolean exigeCid) {
        Procedimento p = new Procedimento();
        p.setCodigo(codigo); p.setDescricao("PROCEDIMENTO DE TESTE " + codigo); p.setModalidade(modalidade);
        p.setExigeCid(exigeCid);
        return procedimentos.save(p).getId();
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, UsuarioAutenticado quem, String corpo) {
        return req.with(user(quem)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private String bpaC(LocalDate data, String quantidade) {
        return """
                {"data":"%s","profissionalId":%d,"procedimentoId":%d,"quantidade":%s,"idade":35}"""
                .formatted(data, profissionalId, procedimentoC, quantidade);
    }

    @Test
    @DisplayName("T11 Primeiro lançamento do mês cria a competência ABERTA")
    void primeiroLancamentoCriaCompetencia() throws Exception {
        assertThat(competencias.findByMesAno(MES)).isEmpty();
        mvc.perform(json(post("/api/atendimentos"), operador, bpaC(MES.atDay(10), "4")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.competencia").value("202503"))
                .andExpect(jsonPath("$.registradoPor").value("Usuário Fictício"));
        Competencia c = competencias.findByMesAno(MES).orElseThrow();
        assertThat(c.getSituacao()).isEqualTo(SituacaoCompetencia.ABERTA);
    }

    @Test
    @DisplayName("T09 Quantidade 0 ou negativa → 400 no campo quantidade")
    void quantidadeInvalida() throws Exception {
        for (String q : new String[] {"0", "-2"}) {
            mvc.perform(json(post("/api/atendimentos"), operador, bpaC(MES.atDay(10), q)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erros[0].campo").value("quantidade"))
                    .andExpect(jsonPath("$.erros[0].mensagem").value("A quantidade deve ser maior que zero."));
        }
    }

    @Test
    @DisplayName("BPA-I completo é lançado com paciente, CID e caráter")
    void bpaICompleto() throws Exception {
        String corpo = """
                {"data":"%s","profissionalId":%d,"procedimentoId":%d,"quantidade":1,"pacienteId":%d,
                 "cid":"J069","caraterAtendimento":"01"}""".formatted(MES.atDay(5), profissionalId, procedimentoI, pacienteId);
        mvc.perform(json(post("/api/atendimentos"), operador, corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pacienteNome").value("JOAO FICTICIO"))
                .andExpect(jsonPath("$.idade").value(35));
    }

    @Test
    @DisplayName("Competência FECHADA recusa lançar, corrigir e excluir")
    void competenciaFechada() throws Exception {
        String resp = mvc.perform(json(post("/api/atendimentos"), operador, bpaC(MES.atDay(10), "1")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(resp.replaceAll(".*\"id\":(\\d+).*", "$1"));

        Competencia c = competencias.findByMesAno(MES).orElseThrow();
        c.setSituacao(SituacaoCompetencia.FECHADA);
        competencias.flush();

        mvc.perform(json(post("/api/atendimentos"), operador, bpaC(MES.atDay(11), "1")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("data"));
        mvc.perform(json(put("/api/atendimentos/" + id), operador, bpaC(MES.atDay(10), "2")))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/atendimentos/" + id).with(user(operador)).with(csrf()))
                .andExpect(status().isBadRequest());
        assertThat(atendimentos.findById(id)).isPresent();
    }

    @Test
    @DisplayName("RF10 Consultar, corrigir e excluir na competência aberta")
    void consultarCorrigirExcluir() throws Exception {
        String resp = mvc.perform(json(post("/api/atendimentos"), operador, bpaC(MES.atDay(10), "1")))
                .andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(resp.replaceAll(".*\"id\":(\\d+).*", "$1"));

        mvc.perform(get("/api/atendimentos").param("competencia", "202503").with(user(operador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(json(put("/api/atendimentos/" + id), operador, bpaC(MES.atDay(12), "7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(7));
        mvc.perform(delete("/api/atendimentos/" + id).with(user(operador)).with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(atendimentos.findById(id)).isEmpty();
    }

    @Test
    @DisplayName("Administrador (sem perfil Operador/Responsável) não lança atendimento → 403")
    void adminNaoLanca() throws Exception {
        mvc.perform(json(post("/api/atendimentos"), admin, bpaC(MES.atDay(10), "1")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Situação da competência para a barra superior")
    void situacaoCompetencia() throws Exception {
        mvc.perform(get("/api/competencias/situacao").param("mesAno", "202503").with(user(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situacao").doesNotExist());
        mvc.perform(json(post("/api/atendimentos"), operador, bpaC(MES.atDay(10), "1")));
        mvc.perform(get("/api/competencias/situacao").param("mesAno", "202503").with(user(admin)))
                .andExpect(jsonPath("$.situacao").value("ABERTA"));
    }
}
