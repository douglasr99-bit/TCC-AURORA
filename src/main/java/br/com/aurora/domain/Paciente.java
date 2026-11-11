package br.com.aurora.domain;

import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Paciente dos atendimentos BPA-I. Só os campos exigidos pelo BPA (LGPD: minimização). */
@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 15, unique = true)
    private String cns;

    @Column(nullable = false, length = 30)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false)
    private Sexo sexo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "raca_cor", nullable = false, length = 2)
    private String racaCor;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 4)
    private String etnia;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String nacionalidade;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "codigo_municipio", nullable = false, length = 6)
    private String codigoMunicipio;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 8)
    private String cep;

    @Column(length = 30)
    private String logradouro;

    @Column(length = 5)
    private String numero;

    @Column(length = 30)
    private String bairro;

    @Column(length = 11)
    private String telefone;

    public Long getId() { return id; }

    public String getCns() { return cns; }
    public void setCns(String cns) { this.cns = cns; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }

    public Sexo getSexo() { return sexo; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }

    public String getRacaCor() { return racaCor; }
    public void setRacaCor(String racaCor) { this.racaCor = racaCor; }

    public String getEtnia() { return etnia; }
    public void setEtnia(String etnia) { this.etnia = etnia; }

    public String getNacionalidade() { return nacionalidade; }
    public void setNacionalidade(String nacionalidade) { this.nacionalidade = nacionalidade; }

    public String getCodigoMunicipio() { return codigoMunicipio; }
    public void setCodigoMunicipio(String codigoMunicipio) { this.codigoMunicipio = codigoMunicipio; }

    public String getCep() { return cep; }
    public void setCep(String cep) { this.cep = cep; }

    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
