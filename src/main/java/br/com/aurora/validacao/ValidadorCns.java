package br.com.aurora.validacao;

/**
 * Validação do Cartão Nacional de Saúde (CNS), conforme o algoritmo publicado pela ANS em
 * "Algoritmos — Aplicativo de carga SIB-XML", seção 4 (gov.br, 07/2026). Ver
 * docs/referencias/ANS_SIB_Algoritmos_2026-07.pdf.
 */
public final class ValidadorCns {

    private ValidadorCns() {
    }

    /** Verdadeiro se {@code cns} tem 15 dígitos e passa na rotina do CNS definitivo ou do provisório. */
    public static boolean valido(String cns) {
        if (cns == null || !cns.matches("\\d{15}") || cns.equals("000000000000000")) {
            return false;
        }
        return validaDefinitivo(cns) || validaProvisorio(cns);
    }

    /** Rotina "validaCns": os 11 primeiros dígitos (base PIS) geram o restante do número. */
    static boolean validaDefinitivo(String cns) {
        String pis = cns.substring(0, 11);
        int soma = somaPonderada(pis);
        int dv = 11 - (soma % 11);
        if (dv == 11) {
            dv = 0;
        }
        String esperado;
        if (dv == 10) {
            soma += 2;
            dv = 11 - (soma % 11);
            esperado = pis + "001" + dv;
        } else {
            esperado = pis + "000" + dv;
        }
        return cns.equals(esperado);
    }

    /** Rotina "validaCnsProv": soma ponderada dos 15 dígitos (pesos 15 a 1) múltipla de 11. */
    static boolean validaProvisorio(String cns) {
        return somaPonderada(cns) % 11 == 0;
    }

    /** Soma dos dígitos multiplicados pelos pesos 15, 14, 13, ... da esquerda para a direita. */
    private static int somaPonderada(String digitos) {
        int soma = 0;
        for (int i = 0; i < digitos.length(); i++) {
            soma += (digitos.charAt(i) - '0') * (15 - i);
        }
        return soma;
    }
}
