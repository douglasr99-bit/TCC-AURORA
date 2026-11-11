package br.com.aurora.domain;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimento_id", nullable = false)
    private Procedimento procedimento;

    /** Nulo nos procedimentos BPA-C. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    /** Usuário que registrou o lançamento (rastreabilidade). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_atendimento", nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private int quantidade;

    private Integer idade;

    @Column(length = 4)
    private String cid;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "carater_atendimento", length = 2)
    private String caraterAtendimento;

    public Long getId() { return id; }

    public Competencia getCompetencia() { return competencia; }
    public void setCompetencia(Competencia competencia) { this.competencia = competencia; }

    public Profissional getProfissional() { return profissional; }
    public void setProfissional(Profissional profissional) { this.profissional = profissional; }

    public Procedimento getProcedimento() { return procedimento; }
    public void setProcedimento(Procedimento procedimento) { this.procedimento = procedimento; }

    public Paciente getPaciente() { return paciente; }
    public void setPaciente(Paciente paciente) { this.paciente = paciente; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public Integer getIdade() { return idade; }
    public void setIdade(Integer idade) { this.idade = idade; }

    public String getCid() { return cid; }
    public void setCid(String cid) { this.cid = cid; }

    public String getCaraterAtendimento() { return caraterAtendimento; }
    public void setCaraterAtendimento(String caraterAtendimento) { this.caraterAtendimento = caraterAtendimento; }
}
