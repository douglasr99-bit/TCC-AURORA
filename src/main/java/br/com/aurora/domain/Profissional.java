package br.com.aurora.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "profissional")
public class Profissional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 15, unique = true)
    private String cns;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 6)
    private String cbo;

    @Column(nullable = false)
    private boolean ativo = true;

    public Long getId() { return id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCns() { return cns; }
    public void setCns(String cns) { this.cns = cns; }

    public String getCbo() { return cbo; }
    public void setCbo(String cbo) { this.cbo = cbo; }

    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
