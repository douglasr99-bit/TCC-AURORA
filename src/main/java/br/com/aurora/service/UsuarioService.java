package br.com.aurora.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Perfil;
import br.com.aurora.domain.Usuario;
import br.com.aurora.dto.UsuarioEntrada;
import br.com.aurora.dto.UsuarioSaida;
import br.com.aurora.exception.Erros;
import br.com.aurora.exception.NaoEncontradoException;
import br.com.aurora.repository.UsuarioRepository;
import br.com.aurora.validacao.Texto;

/** RF02. Um usuário pode ter vários perfis. */
@Service
public class UsuarioService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repositorio, PasswordEncoder encoder) {
        this.repositorio = repositorio;
        this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioSaida> listar() {
        return repositorio.findAllByOrderByNomeAsc().stream().map(UsuarioSaida::de).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioSaida obter(Long id) {
        return UsuarioSaida.de(carregar(id));
    }

    @Transactional
    public UsuarioSaida criar(UsuarioEntrada dados) {
        Erros erros = new Erros();
        if (repositorio.existsByLogin(dados.login())) {
            erros.add("login", "Este login já está em uso. Escolha outro.");
        }
        if (Texto.limpo(dados.senha()) == null) {
            erros.add("senha", "Informe uma senha com pelo menos 8 caracteres.");
        }
        erros.lancarSeHouver();

        Usuario u = new Usuario();
        preencher(u, dados);
        u.setSenhaHash(encoder.encode(dados.senha()));
        return UsuarioSaida.de(repositorio.save(u));
    }

    /**
     * @param idLogado usuário que está fazendo a alteração: ele não pode se desativar nem
     *                 tirar de si o perfil de Administrador (evita ficar sem acesso ao sistema).
     */
    @Transactional
    public UsuarioSaida atualizar(Long id, UsuarioEntrada dados, Long idLogado) {
        Usuario u = carregar(id);
        Erros erros = new Erros();
        if (repositorio.existsByLoginAndIdNot(dados.login(), id)) {
            erros.add("login", "Este login já está em uso por outro usuário.");
        }
        if (id.equals(idLogado)) {
            if (Boolean.FALSE.equals(dados.ativo())) {
                erros.add("ativo", "Você não pode desativar o seu próprio usuário.");
            }
            if (!dados.perfis().contains(Perfil.ADMINISTRADOR)) {
                erros.add("perfis", "Você não pode tirar de si mesmo o perfil de Administrador.");
            }
        }
        erros.lancarSeHouver();

        preencher(u, dados);
        if (Texto.limpo(dados.senha()) != null) {
            u.setSenhaHash(encoder.encode(dados.senha()));
        }
        return UsuarioSaida.de(u);
    }

    private Usuario carregar(Long id) {
        return repositorio.findById(id).orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
    }

    private static void preencher(Usuario u, UsuarioEntrada dados) {
        u.setNome(Texto.limpo(dados.nome()));
        u.setLogin(dados.login());
        u.setPerfis(dados.perfis());
        u.setAtivo(dados.ativo() == null || dados.ativo());
    }
}
