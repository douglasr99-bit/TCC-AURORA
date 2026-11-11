package br.com.aurora.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import br.com.aurora.domain.Perfil;
import br.com.aurora.domain.Usuario;
import br.com.aurora.repository.UsuarioRepository;

/**
 * Primeiro acesso: se não existe nenhum usuário, cria um Administrador com login e senha
 * lidos de AURORA_ADMIN_LOGIN e AURORA_ADMIN_SENHA. Nenhuma senha fica no código.
 */
@Component
@ConditionalOnProperty(name = "aurora.criar-admin-inicial", havingValue = "true", matchIfMissing = true)
public class InicializacaoAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializacaoAdmin.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String login;
    private final String senha;

    public InicializacaoAdmin(UsuarioRepository usuarios, PasswordEncoder encoder,
            @Value("${AURORA_ADMIN_LOGIN:}") String login,
            @Value("${AURORA_ADMIN_SENHA:}") String senha) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.login = login;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            return;
        }
        if (login.isBlank() || senha.isBlank()) {
            throw new IllegalStateException("Nenhum usuário cadastrado. Defina AURORA_ADMIN_LOGIN e "
                    + "AURORA_ADMIN_SENHA para criar o administrador inicial.");
        }
        if (senha.length() < 8) {
            throw new IllegalStateException("AURORA_ADMIN_SENHA deve ter pelo menos 8 caracteres.");
        }
        Usuario admin = new Usuario();
        admin.setNome("Administrador");
        admin.setLogin(login);
        admin.setSenhaHash(encoder.encode(senha));
        admin.setPerfis(Set.of(Perfil.ADMINISTRADOR));
        usuarios.save(admin);
        log.info("Administrador inicial criado.");
    }
}
