package br.com.aurora.domain;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Dados do estabelecimento usados no cabeçalho do BPA. Instância única. */
@Entity
@Table(name = "estabelecimento")
public class Estabelecimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 7, unique = true)
    private String cnes;

    @Column(nullable = false, length = 30)
    private String nome;

    @Column(nullable = false, length = 6)
    private String sigla;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(name = "orgao_destino", nullable = false, length = 40)
    private String orgaoDestino;

    /** M = municipal, E = estadual. */
    @Column(name = "indicador_destino", nullable = false)
    private char indicadorDestino;

    public Long getId() { return id; }

    public String getCnes() { return cnes; }
    public void setCnes(String cnes) { this.cnes = cnes; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getSigla() { return sigla; }
    public void setSigla(String sigla) { this.sigla = sigla; }

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }

    public String getOrgaoDestino() { return orgaoDestino; }
    public void setOrgaoDestino(String orgaoDestino) { this.orgaoDestino = orgaoDestino; }

    public char getIndicadorDestino() { return indicadorDestino; }
    public void setIndicadorDestino(char indicadorDestino) { this.indicadorDestino = indicadorDestino; }
}
