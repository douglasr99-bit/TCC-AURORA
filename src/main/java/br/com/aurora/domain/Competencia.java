package br.com.aurora.domain;

import java.time.LocalDateTime;
import java.time.YearMonth;

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
@Table(name = "competencia")
public class Competencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "mes_ano", nullable = false, length = 6, unique = true)
    private YearMonth mesAno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SituacaoCompetencia situacao = SituacaoCompetencia.ABERTA;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    public Long getId() { return id; }

    public YearMonth getMesAno() { return mesAno; }
    public void setMesAno(YearMonth mesAno) { this.mesAno = mesAno; }

    public SituacaoCompetencia getSituacao() { return situacao; }
    public void setSituacao(SituacaoCompetencia situacao) { this.situacao = situacao; }

    public LocalDateTime getDataFechamento() { return dataFechamento; }
    public void setDataFechamento(LocalDateTime dataFechamento) { this.dataFechamento = dataFechamento; }
}
