package br.com.aurora.bpa;

/**
 * Registro de cabeçalho do BPA (layout DATASUS): 130 posições + CR LF.
 *
 * <pre>
 * 001-002 cbc-hdr      NUM  2  "01"
 * 003-007 cbc-hdr      ALFA 5  "#BPA#"
 * 008-013 cbc-mvm      NUM  6  ano e mês de processamento (AAAAMM)
 * 014-019 cbc-lin      NUM  6  número de linhas de BPA gravadas
 * 020-025 cbc-flh      NUM  6  quantidade de folhas de BPA gravadas
 * 026-029 cbc-smt-vrf  NUM  4  campo de controle [1111..2221]
 * 030-059 cbc-rsp      ALFA 30 nome do órgão de origem
 * 060-065 cbc-sgl      ALFA 6  sigla do órgão de origem
 * 066-079 cbc-cgccpf   NUM  14 CNPJ/CPF do prestador
 * 080-119 cbc-dst      ALFA 40 nome do órgão de destino
 * 120-120 cbc-dst-in   ALFA 1  M = municipal, E = estadual
 * 121-130 cbc_versao   ALFA 10 versão do sistema (livre)
 * 131-132 cbc-fim      CR LF
 * </pre>
 */
public record Cabecalho(
        String mesAno,
        int linhas,
        int folhas,
        int controle,
        String nomeOrigem,
        String siglaOrigem,
        String cnpj,
        String orgaoDestino,
        char indicadorDestino,
        String versao) {

    public static final int TAMANHO = 130;

    public String linha() {
        String l = "01"
                + "#BPA#"
                + Campos.num(mesAno, 6, "cbc-mvm")
                + Campos.num(linhas, 6, "cbc-lin")
                + Campos.num(folhas, 6, "cbc-flh")
                + Campos.num(controle, 4, "cbc-smt-vrf")
                + Campos.alfa(nomeOrigem, 30, "cbc-rsp")
                + Campos.alfa(siglaOrigem, 6, "cbc-sgl")
                + Campos.num(cnpj, 14, "cbc-cgccpf")
                + Campos.alfa(orgaoDestino, 40, "cbc-dst")
                + Campos.alfa(String.valueOf(indicadorDestino), 1, "cbc-dst-in")
                + Campos.alfa(versao, 10, "cbc_versao");
        return l;
    }
}
