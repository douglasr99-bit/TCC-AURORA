package br.com.aurora.config;

import java.io.IOException;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import br.com.aurora.exception.RespostaErro;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Autenticação por formulário com sessão no servidor (sem JWT), senhas em BCrypt,
 * CSRF ativo e autorização por perfil verificada no servidor, rota a rota.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    static final String ADMIN = "ADMINISTRADOR";
    static final String OPERADOR = "OPERADOR";
    static final String RESPONSAVEL = "RESPONSAVEL_PRODUCAO";

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, JsonMapper json) throws Exception {
        RequestMatcher api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");

        http
            .authorizeHttpRequests(a -> a
                // Página de login e arquivos estáticos (sem dados)
                .requestMatchers("/login.html", "/css/**", "/js/**", "/img/**", "/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/csrf").permitAll()
                // RF02
                .requestMatchers("/api/usuarios/**").hasRole(ADMIN)
                // RF03
                .requestMatchers(HttpMethod.GET, "/api/estabelecimento").authenticated()
                .requestMatchers("/api/estabelecimento/**").hasRole(ADMIN)
                // RF04 e RF06: leitura para todos os perfis, escrita só Administrador
                .requestMatchers(HttpMethod.GET, "/api/profissionais/**", "/api/procedimentos/**").authenticated()
                .requestMatchers("/api/profissionais/**", "/api/procedimentos/**").hasRole(ADMIN)
                // RF05: o Responsável também lança atendimentos (RF10) e precisa buscar o paciente
                .requestMatchers(HttpMethod.GET, "/api/pacientes/**").hasAnyRole(OPERADOR, RESPONSAVEL)
                .requestMatchers("/api/pacientes/**").hasRole(OPERADOR)
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .usernameParameter("login")
                .passwordParameter("senha")
                .successHandler((req, res, auth) -> {
                    UsuarioAutenticado u = (UsuarioAutenticado) auth.getPrincipal();
                    escrever(res, HttpStatus.OK, json, Map.of("nome", u.getNome(), "perfis", u.getPerfis()));
                })
                // Mensagem genérica: não revela se o login existe nem se está inativo.
                .failureHandler((req, res, ex) -> escrever(res, HttpStatus.UNAUTHORIZED, json,
                        RespostaErro.de(null, "Login ou senha incorretos.")))
                .permitAll())
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
            // Token CSRF em cookie legível pelo JavaScript (XSRF-TOKEN), enviado no cabeçalho X-XSRF-TOKEN.
            .csrf(c -> c.spa())
            .exceptionHandling(e -> e
                .defaultAuthenticationEntryPointFor((req, res, ex) -> escrever(res, HttpStatus.UNAUTHORIZED, json,
                        RespostaErro.de(null, "Sua sessão expirou. Entre novamente.")), api)
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login.html"),
                        AnyRequestMatcher.INSTANCE)
                .defaultAccessDeniedHandlerFor((req, res, ex) -> escrever(res, HttpStatus.FORBIDDEN, json,
                        RespostaErro.de(null, ex instanceof CsrfException
                                ? "A página ficou aberta por muito tempo. Recarregue a página e tente de novo."
                                : "Seu perfil não tem permissão para esta ação.")), api));

        return http.build();
    }

    private static void escrever(HttpServletResponse res, HttpStatus status, JsonMapper json, Object corpo)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        json.writeValue(res.getOutputStream(), corpo);
    }
}
