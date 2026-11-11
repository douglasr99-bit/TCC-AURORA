package br.com.aurora.bpa;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Registro BPA-I (individualizado), layout DATASUS: 351 posições + CR LF.
 *
 * <pre>
 * 001-002 prd-ident        NUM  2  "03"
 * 003-009 prd-cnes         NUM  7  CNES
 * 010-015 prd-cmp          NUM  6  competência (AAAAMM)
 * 016-030 prd_cnsmed       NUM  15 CNS do profissional
 * 031-036 prd_cbo          ALFA 6  CBO
 * 037-044 prd_dtaten       NUM  8  data do atendimento (AAAAMMDD)
 * 045-047 prd-flh          NUM  3  folha
 * 048-049 prd-seq          NUM  2  linha na folha [01..99]
 * 050-059 prd-pa           NUM  10 procedimento
 * 060-074 prd-cnspac       NUM  15 CNS do paciente
 * 075-075 prd-sexo         ALFA 1  M/F
 * 076-081 prd-ibge         NUM  6  município de residência
 * 082-085 prd-cid          ALFA 4  CID-10
 * 086-088 prd-idade        NUM  3  idade
 * 089-094 prd-qt           NUM  6  quantidade
 * 095-096 prd-caten        NUM  2  caráter de atendimento
 * 097-109 prd-naut         NUM  13 nº da autorização (brancos)
 * 110-112 prd-org          ALFA 3  "BPA"
 * 113-142 prd-nmpac        ALFA 30 nome do paciente
 * 143-150 prd-dtnasc       NUM  8  nascimento (AAAAMMDD)
 * 151-152 prd-raca         NUM  2  raça/cor
 * 153-156 prd-etnia        NUM  4  etnia (só raça/cor 05)
 * 157-159 prd-nac          NUM  3  nacionalidade
 * 160-162 prd_srv          NUM  3  serviço (brancos)
 * 163-165 prd_clf          NUM  3  classificação (brancos)
 * 166-173 prd_equipe_Seq   NUM  8  (brancos)
 * 174-177 prd_equipe_Area  NUM  4  (brancos)
 * 178-191 prd_cnpj         NUM  14 CNPJ manutenção OPM (brancos)
 * 192-199 prd_cep_pcnte    NUM  8  CEP
 * 200-202 prd_lograd_pcnte NUM  3  código do logradouro (brancos)
 * 203-232 prd_end_pcnte    ALFA 30 endereço
 * 233-242 prd_compl_pcnte  ALFA 10 complemento (brancos)
 * 243-247 prd_num_pcnte    ALFA 5  número (padrão "SN")
 * 248-277 prd_bairro_pcnte ALFA 30 bairro
 * 278-288 prd_ddtel_pcnte  NUM  11 telefone
 * 289-328 prd_email_pcnte  ALFA 40 e-mail (brancos)
 * 329-338 prd_ine          NUM  10 equipe (brancos — fora do escopo)
 * 339-349 prd_cpf_pcnte    NUM  11 CPF (brancos: paciente identificado pelo CNS)
 * 350-350 prd_situacao_rua ALFA 1  (branco)
 * 351-351 prd_sem_cpf      ALFA 1  (branco)
 * 352-353 prd-fim          CR LF
 * </pre>
 */
public record RegistroBpaI(
        String cnes,
        String competencia,
        String cnsProfissional,
        String cbo,
        LocalDate dataAtendimento,
        int folha,
        int sequencia,
        String procedimento,
        String cnsPaciente,
        char sexo,
        String municipio,
        String cid,
        int idade,
        int quantidade,
        String caraterAtendimento,
        String nomePaciente,
        LocalDate dataNascimento,
        String racaCor,
        String etnia,
        String nacionalidade,
        String cep,
        String endereco,
        String numero,
        String bairro,
        String telefone) {

    public static final int TAMANHO = 351;
    public static final int LINHAS_POR_FOLHA = 99;

    private static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.BASIC_ISO_DATE;

    public String linha() {
        return "03"
                + Campos.num(cnes, 7, "prd-cnes")
                + Campos.num(competencia, 6, "prd-cmp")
                + Campos.num(cnsProfissional, 15, "prd_cnsmed")
                + Campos.alfa(cbo, 6, "prd_cbo")
                + dataAtendimento.format(AAAAMMDD)
                + Campos.num(folha, 3, "prd-flh")
                + Campos.num(sequencia, 2, "prd-seq")
                + Campos.num(procedimento, 10, "prd-pa")
                + Campos.num(cnsPaciente, 15, "prd-cnspac")
                + Campos.alfa(String.valueOf(sexo), 1, "prd-sexo")
                + Campos.num(municipio, 6, "prd-ibge")
                + Campos.alfa(cid, 4, "prd-cid")
                + Campos.num(idade, 3, "prd-idade")
                + Campos.num(quantidade, 6, "prd-qt")
                + Campos.numOuBrancos(caraterAtendimento, 2, "prd-caten")
                + Campos.brancos(13)                                   // prd-naut
                + "BPA"
                + Campos.alfa(nomePaciente, 30, "prd-nmpac")
                + dataNascimento.format(AAAAMMDD)
                + Campos.num(racaCor, 2, "prd-raca")
                + Campos.alfa(etnia, 4, "prd-etnia")                   // aceita X999 da tabela vigente do SIA
                + Campos.num(nacionalidade, 3, "prd-nac")
                + Campos.brancos(3)                                    // prd_srv
                + Campos.brancos(3)                                    // prd_clf
                + Campos.brancos(8)                                    // prd_equipe_Seq
                + Campos.brancos(4)                                    // prd_equipe_Area
                + Campos.brancos(14)                                   // prd_cnpj
                + Campos.numOuBrancos(cep, 8, "prd_cep_pcnte")
                + Campos.brancos(3)                                    // prd_lograd_pcnte
                + Campos.alfa(endereco, 30, "prd_end_pcnte")
                + Campos.brancos(10)                                   // prd_compl_pcnte
                + Campos.alfa(numero == null || numero.isBlank() ? "SN" : numero, 5, "prd_num_pcnte")
                + Campos.alfa(bairro, 30, "prd_bairro_pcnte")
                + Campos.alfa(telefone, 11, "prd_ddtel_pcnte")         // brancos à direita se não preenchido
                + Campos.brancos(40)                                   // prd_email_pcnte
                + Campos.brancos(10)                                   // prd_ine
                + Campos.brancos(11)                                   // prd_cpf_pcnte
                + Campos.brancos(1)                                    // prd_situacao_rua
                + Campos.brancos(1);                                   // prd_sem_cpf
    }
}
