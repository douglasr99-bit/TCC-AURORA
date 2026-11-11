package br.com.aurora.config;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Component;

/**
 * Encerra as sessões abertas de um usuário. Usado quando ele é desativado ou tem perfis ou
 * senha alterados: o acesso muda na hora, sem esperar o próximo login.
 */
@Component
public class SessoesUsuario {

    private final SessionRegistry registro;

    public SessoesUsuario(SessionRegistry registro) {
        this.registro = registro;
    }

    public void encerrar(Long usuarioId) {
        for (Object principal : registro.getAllPrincipals()) {
            if (principal instanceof UsuarioAutenticado u && u.getId().equals(usuarioId)) {
                registro.getAllSessions(principal, false).forEach(SessionInformation::expireNow);
            }
        }
    }
}
