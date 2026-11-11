package br.com.aurora.domain;

import java.time.LocalDateTime;

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
@Table(name = "reabertura_competencia")
public class ReaberturaCompetencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competencia_id", nullable = false)
    private Competencia competencia;

    /** Usuário que reabriu. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "data_reabertura", nullable = false)
    private LocalDateTime dataReabertura;

    @Column(nullable = false, length = 255)
    private String motivo;

    public Long getId() { return id; }

    public Competencia getCompetencia() { return competencia; }
    public void setCompetencia(Competencia competencia) { this.competencia = competencia; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public LocalDateTime getDataReabertura() { return dataReabertura; }
    public void setDataReabertura(LocalDateTime dataReabertura) { this.dataReabertura = dataReabertura; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
