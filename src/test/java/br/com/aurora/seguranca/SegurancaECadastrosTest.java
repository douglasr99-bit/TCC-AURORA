package br.com.aurora.seguranca;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.config.UsuarioAutenticado;
import br.com.aurora.domain.Perfil;
import br.com.aurora.domain.Usuario;
import br.com.aurora.repository.UsuarioRepository;

/**
 * Testes de integração (banco PostgreSQL de teste): autenticação, autorização por perfil,
 * CSRF e erros de validação por campo. Cada teste é desfeito ao final (rollback).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SegurancaECadastrosTest {

    private static final String SENHA = "senha-ficticia-123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired PasswordEncoder encoder;

    private Usuario admin;
    private Usuario operador;

    @BeforeEach
    void preparar() {
        admin = criarUsuario("it.admin", Perfil.ADMINISTRADOR, true);
        operador = criarUsuario("it.operador", Perfil.OPERADOR, true);
    }

    private Usuario criarUsuario(String login, Perfil perfil, boolean ativo) {
        Usuario u = new Usuario();
        u.setNome("Usuário Fictício " + login);
        u.setLogin(login);
        u.setSenhaHash(encoder.encode(SENHA));
        u.setPerfis(Set.of(perfil));
        u.setAtivo(ativo);
        return usuarios.saveAndFlush(u);
    }

    // ---------- RF01: autenticação ----------

    @Test
    @DisplayName("T01 Login válido → 200 com nome e perfis")
    void loginValido() throws Exception {
        mvc.perform(post("/login").param("login", "it.operador").param("senha", SENHA).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfis", hasItem("OPERADOR")));
    }

    @Test
    @DisplayName("T02 Senha errada → 401 com mensagem genérica")
    void senhaErrada() throws Exception {
        mvc.perform(post("/login").param("login", "it.operador").param("senha", "errada").with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Login ou senha incorretos."));
    }

    @Test
    @DisplayName("T02b Login inexistente → mesma mensagem genérica (não revela se o login existe)")
    void loginInexistente() throws Exception {
        mvc.perform(post("/login").param("login", "nao.existe").param("senha", SENHA).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Login ou senha incorretos."));
    }

    @Test
    @DisplayName("T03 Usuário inativo não entra → 401 com a mesma mensagem genérica")
    void usuarioInativo() throws Exception {
        criarUsuario("it.inativo", Perfil.OPERADOR, false);
        mvc.perform(post("/login").param("login", "it.inativo").param("senha", SENHA).with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Login ou senha incorretos."));
    }

    @Test
    @DisplayName("Sem login, a API responde 401 e as páginas redirecionam para o login")
    void semLogin() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/cadastros.html")).andExpect(status().is3xxRedirection());
    }

    // ---------- RNF04: autorização por perfil, verificada no servidor ----------

    @Test
    @DisplayName("T04 Operador acessando cadastro de usuários → 403")
    void operadorSemPermissao() throws Exception {
        mvc.perform(get("/api/usuarios").with(user(new UsuarioAutenticado(operador))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erros[0].mensagem").value("Seu perfil não tem permissão para esta ação."));
    }

    @Test
    @DisplayName("T04b Operador pode ler o estabelecimento, mas não alterar → 403")
    void operadorNaoAlteraEstabelecimento() throws Exception {
        mvc.perform(get("/api/estabelecimento").with(user(new UsuarioAutenticado(operador))))
                .andExpect(status().is2xxSuccessful());
        mvc.perform(put("/api/estabelecimento").with(user(new UsuarioAutenticado(operador))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("T04c Administrador sem perfil Operador não acessa pacientes → 403")
    void adminSemAcessoAPacientes() throws Exception {
        mvc.perform(get("/api/pacientes").with(user(new UsuarioAutenticado(admin))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Requisição que altera dados sem token CSRF → 403")
    void semCsrf() throws Exception {
        mvc.perform(post("/api/procedimentos").with(user(new UsuarioAutenticado(admin)))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    // ---------- RF08/RF09: erros de validação junto ao campo ----------

    @Test
    @DisplayName("T08 CNS do paciente com 14 dígitos → 400 com erro no campo cns")
    void cnsComDigitosAMenos() throws Exception {
        String corpo = """
                {"cns":"10000000000000","nome":"JOAO FICTICIO","dataNascimento":"1980-01-01","sexo":"M",
                 "racaCor":"01","nacionalidade":"010","codigoMunicipio":"355030"}""";
        mvc.perform(post("/api/pacientes").with(user(new UsuarioAutenticado(operador))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("cns"))
                .andExpect(jsonPath("$.erros[0].mensagem").value(
                        "O CNS deve ter 15 dígitos, só números. Confira o cartão do paciente."));
    }

    @Test
    @DisplayName("Paciente com nome acima de 30 caracteres (limite do BPA) → recusado, não truncado")
    void nomeAcimaDoLayout() throws Exception {
        String corpo = """
                {"cns":"100000000000007","nome":"JOAO FICTICIO DE SOUZA PEREIRA SANTOS","dataNascimento":"1980-01-01",
                 "sexo":"M","racaCor":"01","nacionalidade":"010","codigoMunicipio":"355030"}""";
        mvc.perform(post("/api/pacientes").with(user(new UsuarioAutenticado(operador))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("nome"));
    }

    @Test
    @DisplayName("Paciente válido é cadastrado → 201")
    void pacienteValido() throws Exception {
        String corpo = """
                {"cns":"100000000000007","nome":"JOAO FICTICIO","dataNascimento":"1980-01-01","sexo":"M",
                 "racaCor":"01","nacionalidade":"010","codigoMunicipio":"355030"}""";
        mvc.perform(post("/api/pacientes").with(user(new UsuarioAutenticado(operador))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    @DisplayName("T09 Código de procedimento com dígito verificador inválido → 400 no campo codigo")
    void procedimentoDvInvalido() throws Exception {
        mvc.perform(post("/api/procedimentos").with(user(new UsuarioAutenticado(admin))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"0301010071\",\"descricao\":\"TESTE\",\"modalidade\":\"BPA_I\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("codigo"));
    }

    @Test
    @DisplayName("Estabelecimento com vários erros → todos os campos são apontados de uma vez")
    void variosErros() throws Exception {
        mvc.perform(put("/api/estabelecimento").with(user(new UsuarioAutenticado(admin))).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnes\":\"12\",\"nome\":\"\",\"sigla\":\"X\",\"cnpj\":\"1\",\"orgaoDestino\":\"SMS\",\"indicadorDestino\":\"Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.length()").value(4));
    }
}
