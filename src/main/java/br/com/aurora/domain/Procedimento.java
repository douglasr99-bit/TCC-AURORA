package br.com.aurora.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "procedimento")
public class Procedimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 10, unique = true)
    private String codigo;

    @Column(nullable = false, length = 250)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Modalidade modalidade;

    @Column(name = "exige_cid", nullable = false)
    private boolean exigeCid;

    public Long getId() { return id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public Modalidade getModalidade() { return modalidade; }
    public void setModalidade(Modalidade modalidade) { this.modalidade = modalidade; }

    public boolean isExigeCid() { return exigeCid; }
    public void setExigeCid(boolean exigeCid) { this.exigeCid = exigeCid; }
}
