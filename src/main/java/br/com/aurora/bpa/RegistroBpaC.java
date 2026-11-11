package br.com.aurora.bpa;

/**
 * Registro BPA-C (consolidado), layout DATASUS: 48 posições + CR LF.
 *
 * <pre>
 * 001-002 prd-ident NUM  2  "02"
 * 003-009 prd-cnes  NUM  7  CNES
 * 010-015 prd-cmp   NUM  6  competência de realização (AAAAMM)
 * 016-021 prd_cbo   ALFA 6  CBO do profissional
 * 022-024 prd-flh   NUM  3  folha [001..999]
 * 025-026 prd-seq   NUM  2  linha na folha [01..20]
 * 027-036 prd-pa    NUM  10 procedimento (com dígito verificador)
 * 037-039 prd-idade NUM  3  idade [0..130]
 * 040-045 prd-qt    NUM  6  quantidade
 * 046-048 prd-org   ALFA 3  origem "BPA"
 * 049-050 prd-fim   CR LF
 * </pre>
 */
public record RegistroBpaC(
        String cnes,
        String competencia,
        String cbo,
        int folha,
        int sequencia,
        String procedimento,
        int idade,
        int quantidade) {

    public static final int TAMANHO = 48;
    public static final int LINHAS_POR_FOLHA = 20;

    public String linha() {
        return "02"
                + Campos.num(cnes, 7, "prd-cnes")
                + Campos.num(competencia, 6, "prd-cmp")
                + Campos.alfa(cbo, 6, "prd_cbo")
                + Campos.num(folha, 3, "prd-flh")
                + Campos.num(sequencia, 2, "prd-seq")
                + Campos.num(procedimento, 10, "prd-pa")
                + Campos.num(idade, 3, "prd-idade")
                + Campos.num(quantidade, 6, "prd-qt")
                + "BPA";
    }
}
