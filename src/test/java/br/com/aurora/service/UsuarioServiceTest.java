package br.com.aurora.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.aurora.domain.Perfil;
import br.com.aurora.domain.Usuario;
import br.com.aurora.dto.UsuarioEntrada;
import br.com.aurora.exception.NegocioException;
import br.com.aurora.repository.UsuarioRepository;

class UsuarioServiceTest {

    private UsuarioRepository repositorio;
    private UsuarioService servico;
    private Usuario admin;

    @BeforeEach
    void preparar() {
        repositorio = mock(UsuarioRepository.class);
        servico = new UsuarioService(repositorio, new BCryptPasswordEncoder());
        admin = new Usuario();
        ReflectionTestUtils.setField(admin, "id", 1L);
        admin.setNome("Admin Ficticio");
        admin.setLogin("admin.teste");
        admin.setSenhaHash("x");
        admin.setPerfis(Set.of(Perfil.ADMINISTRADOR));
        when(repositorio.findById(1L)).thenReturn(Optional.of(admin));
    }

    @Test
    @DisplayName("Administrador não pode desativar a si mesmo")
    void naoDesativaASiMesmo() {
        var dados = new UsuarioEntrada("Admin Ficticio", "admin.teste", null, Set.of(Perfil.ADMINISTRADOR), false);
        var e = assertThrows(NegocioException.class, () -> servico.atualizar(1L, dados, 1L));
        assertThat(e.getErros()).anyMatch(x -> "ativo".equals(x.campo()));
    }

    @Test
    @DisplayName("Administrador não pode tirar de si o perfil de Administrador")
    void naoTiraProprioPerfilAdmin() {
        var dados = new UsuarioEntrada("Admin Ficticio", "admin.teste", null, Set.of(Perfil.OPERADOR), true);
        var e = assertThrows(NegocioException.class, () -> servico.atualizar(1L, dados, 1L));
        assertThat(e.getErros()).anyMatch(x -> "perfis".equals(x.campo()));
    }

    @Test
    @DisplayName("Edição sem senha mantém a senha atual; com senha, grava novo hash BCrypt")
    void senhaNaEdicao() {
        servico.atualizar(1L, new UsuarioEntrada("Admin Ficticio", "admin.teste", null,
                Set.of(Perfil.ADMINISTRADOR), true), 1L);
        assertThat(admin.getSenhaHash()).isEqualTo("x");

        servico.atualizar(1L, new UsuarioEntrada("Admin Ficticio", "admin.teste", "novaSenhaFicticia",
                Set.of(Perfil.ADMINISTRADOR), true), 1L);
        assertThat(admin.getSenhaHash()).startsWith("$2");
    }

    @Test
    @DisplayName("Criação sem senha → erro no campo senha")
    void criarSemSenha() {
        var dados = new UsuarioEntrada("Operador Ficticio", "operador.teste", null, Set.of(Perfil.OPERADOR), true);
        var e = assertThrows(NegocioException.class, () -> servico.criar(dados));
        assertThat(e.getErros()).anyMatch(x -> "senha".equals(x.campo()));
    }
}
