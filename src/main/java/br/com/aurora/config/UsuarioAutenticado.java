package br.com.aurora.config;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.com.aurora.domain.Perfil;
import br.com.aurora.domain.Usuario;

/** Usuário guardado na sessão. Cada perfil vira um papel ROLE_&lt;PERFIL&gt;. */
public final class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String nome;
    private final String login;
    private final String senhaHash;
    private final Set<Perfil> perfis;
    private final boolean ativo;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.login = usuario.getLogin();
        this.senhaHash = usuario.getSenhaHash();
        this.perfis = Set.copyOf(usuario.getPerfis());
        this.ativo = usuario.isAtivo();
    }

    public Long getId() { return id; }

    public String getNome() { return nome; }

    public Set<Perfil> getPerfis() { return perfis; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<SimpleGrantedAuthority> papeis = perfis.stream()
                .map(p -> new SimpleGrantedAuthority("ROLE_" + p.name()))
                .toList();
        return papeis;
    }

    @Override
    public String getPassword() { return senhaHash; }

    @Override
    public String getUsername() { return login; }

    /** Usuário com ativo = false não entra. */
    @Override
    public boolean isEnabled() { return ativo; }

    /** Mesmo usuário = mesmo id (o registro de sessões agrupa as sessões por usuário). */
    @Override
    public boolean equals(Object outro) {
        return outro instanceof UsuarioAutenticado u && id.equals(u.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
